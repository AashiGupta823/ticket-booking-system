# System Design Write-Up

## Seat Hold & TTL Mechanism

Every physical `Seat` in a venue gets a corresponding `ShowSeat` row when an
organiser creates a show — one row per (show, seat), holding a live
`status` (`AVAILABLE` / `HELD` / `BOOKED`). This row is the single source of
truth for seat availability; the frontend seat map polls it every 4 seconds.

When a customer clicks a seat, `POST /seats/hold` creates a `SeatHold` row
(`showSeatId`, `customerId`, `createdAt`, `expiresAt = now + TTL`, default
10 minutes, configurable via `SEAT_HOLD_TTL_MINUTES`) and flips the
`ShowSeat` to `HELD`. The TTL is enforced two ways, deliberately redundant:

1. **On read/write:** `confirmBookingFromHolds()` re-checks `expiresAt`
   before honouring a hold, so even a hold that lapsed seconds ago is
   rejected rather than silently booked.
2. **Proactively:** a `@Scheduled` job (`SeatHoldCleanupScheduler`) runs
   every 30 seconds, finds every hold past `expiresAt` that was never
   `consumed` by a completed booking, and flips the seat back to
   `AVAILABLE`. This is what makes an abandoned checkout self-heal without
   any customer action — the seat map shows it as available again within
   one polling cycle of both the scheduler and the frontend.

`consumed` on `SeatHold` distinguishes "converted into a booking" from
"expired/released", so the cleanup job never touches a seat that was
successfully booked in the meantime.

## Concurrency Prevention

The requirement is strict: two customers hitting the same seat within
milliseconds must never both succeed. Optimistic locking alone (a `@Version`
column) is not sufficient here because it only *detects* a conflict after
the fact — one request would still get a confusing failure mid-checkout
rather than a clean "seat unavailable" at selection time. Instead, the hold
and booking paths use **pessimistic row locking**:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select ss from ShowSeat ss where ss.id = :id")
Optional<ShowSeat> findByIdForUpdate(@Param("id") Long id);
```

This issues `SELECT ... FOR UPDATE` inside a single `@Transactional` method.
If two requests for the same seat arrive concurrently: the first transaction
acquires the row lock, reads `AVAILABLE`, writes `HELD`, and commits — at
which point the lock is released. The second transaction's identical query
**blocks at the database level** while waiting for that lock; only once it's
granted does it re-read the row, now seeing `HELD`, and correctly throws
`SeatUnavailableException` (mapped to HTTP 409). No application-level
mutex or distributed lock is needed — MySQL's InnoDB row locking does the
serialization for us, and it holds even across multiple app instances,
which an in-process lock would not.

The `@Version` field on `ShowSeat` is a secondary safety net: if any future
code path ever mutates the row without going through `findByIdForUpdate()`,
Hibernate's optimistic check will still catch the lost update and throw
rather than silently corrupting state.

## Waitlist Auto-Assignment Flow

`Waitlist` rows form a FIFO queue per `(show, category)`, ordered by
`joinedAt`. When a booking is cancelled, `BookingService.cancelBooking()`
calls `WaitlistService.offerNextInLine(seat)` for every seat freed by that
cancellation. This method:

1. Locks the freed `ShowSeat` (same `findByIdForUpdate` pattern).
2. Looks up the earliest `WAITING` entry for that show+category.
3. If none exists, releases the seat to `AVAILABLE` for general sale.
4. If one exists, marks it `OFFERED`, sets the seat back to `HELD`
   (reserved, not generally purchasable), and creates a `WaitlistOffer` —
   a random token, `createdAt`, and `expiresAt = now + offer TTL` (default
   15 minutes) — then emails the customer a link containing that token.

This reuses the same "seat is HELD, protected by the same locking
mechanism" pattern as a normal hold, rather than inventing a parallel
availability model — one less thing to keep consistent.

## Time-Limited Offer Handling

`GET /waitlist/offer/{token}` lets the customer preview the offer (show,
seat, expiry) before authenticating. `POST /waitlist/offer/{token}/accept`
validates the token is still `PENDING` and not expired, confirms it belongs
to the authenticated customer, and hands off to
`BookingService.confirmBookingFromWaitlistOffer()`, which re-locks the seat,
flips it to `BOOKED`, and proceeds through the same QR-generation and
email-confirmation path as a normal booking.

If the customer does nothing, a second scheduled job
(`WaitlistOfferCleanupScheduler`, same 30-second interval) finds offers past
their `expiresAt` still `PENDING`, marks them `EXPIRED`, marks the
underlying `Waitlist` entry `EXPIRED` too, and — critically — immediately
calls `offerNextInLine()` again for that same seat. This cascades the offer
to whoever is now first in the (updated) queue, or releases the seat if
nobody is left waiting. The customer never has to poll for this; the next
offer email simply arrives on its own schedule.

*(Word count: ~790)*
