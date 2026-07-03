# Ticket Booking System

**🔗 Live App:** https://ticket-booking-system-xln2-git-main-aashi-gupta-s-projects.vercel.app
**🔗 Backend API:** https://ticket-booking-system-i5jf.onrender.com

> Note: Backend is on Render's free tier and may take 30-50 seconds to respond on first request after inactivity.

A movie/concert ticket booking platform with a visual seat map, TTL-based seat
holds, concurrency-safe booking, and an automatic waitlist with time-limited
offers on cancellation.
## Tech Stack
- **Backend:** Spring Boot 3.4.5, Java 21, Spring Data JPA, Spring Security (JWT)
- **Database:** MySQL 8
- **Frontend:** React 18 + Vite, React Router
- **QR codes:** ZXing
- **Email:** Spring Mail (SMTP, works with any free-tier provider e.g. Gmail App Password)

## Project Structure
```
ticket-booking-system/
├── src/main/java/com/tbs/
│   ├── entity/        # JPA entities
│   ├── repository/    # Spring Data repositories (incl. pessimistic-lock query)
│   ├── service/        # SeatHoldService, WaitlistService, BookingService, Email, QR
│   ├── controller/     # REST controllers
│   ├── security/       # JWT filter + Spring Security config
│   ├── scheduler/      # TTL cleanup jobs
│   └── dto/             # Request/response records
├── src/main/resources/application.yml
├── frontend/            # React + Vite app
├── .env.example
└── SYSTEM_DESIGN.md
```

## Setup Guide

### 1. Database
```sql
CREATE DATABASE ticket_booking;
```
Tables are auto-created by Hibernate (`ddl-auto: update`) on first run.

### 2. Backend
```bash
cp .env.example .env      # fill in DB creds, JWT secret, SMTP creds
# export the vars in .env into your shell, or use IntelliJ's EnvFile plugin,
# or `set -a; source .env; set +a` on a Unix shell
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`.

**Gmail SMTP note:** use a 16-character [App Password](https://myaccount.google.com/apppasswords)
(not your normal Gmail password) — requires 2FA enabled on the Google account.

### 3. Frontend
```bash
cd frontend
cp .env.example .env      # set VITE_API_BASE_URL
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`.

### 4. First-run walkthrough
1. Register a user with role `ADMIN` → create a venue (name, categories, seat rows).
2. Register a user with role `ORGANISER` → create a show against that venue, set per-category pricing.
3. Register a `CUSTOMER` → browse events → open a show → click a seat to hold it → confirm booking → check email for the QR ticket.
4. To see the waitlist flow: book every seat in a category as different customers, join the waitlist as one more customer, then cancel one booking — the waitlisted customer gets an email with a time-limited offer link.

## Database Schema (core tables)
| Table | Purpose |
|---|---|
| `users` | customer / organiser / admin, bcrypt password hash |
| `venues`, `seat_categories`, `seats` | venue layout, physical seats tied to a category |
| `shows`, `show_category_prices` | event listings, per-category pricing |
| `show_seats` | **one row per (show, seat)** — carries the live status (`AVAILABLE` / `HELD` / `BOOKED`) and a `@Version` column. This is the row that gets locked during hold/booking. |
| `seat_holds` | active TTL holds: `show_seat_id`, `customer_id`, `expires_at`, `consumed` |
| `bookings`, `booking_seats` | confirmed bookings and the seats within them |
| `waitlist_entries` | FIFO queue per (show, category), `joined_at` for ordering |
| `waitlist_offers` | time-limited offer tying a queue entry to a specific freed seat, with `offer_token` and `expires_at` |

## Seat Hold TTL & Auto-Release
- `POST /api/shows/{id}/seats/hold` takes a pessimistic row lock on the target
  `show_seats` row, checks it's `AVAILABLE`, flips it to `HELD`, and creates a
  `SeatHold` row with `expiresAt = now + SEAT_HOLD_TTL_MINUTES` (default 10).
- A `@Scheduled` job (`SeatHoldCleanupScheduler`) runs every `SEAT_HOLD_CLEANUP_MS`
  (default 30s), finds every hold past its `expiresAt` that was never `consumed`
  by a booking, and releases the seat back to `AVAILABLE`. No customer action
  needed — this is what makes an abandoned checkout self-heal.
- The frontend polls the seat map every 4s so the grid reflects releases and
  other customers' holds without a manual refresh.

## Concurrency Protection
Two customers clicking the same seat within milliseconds must never both
succeed. This is handled by `ShowSeatRepository.findByIdForUpdate()`, which
issues `SELECT ... FOR UPDATE` inside a single `@Transactional` method:
1. Request A's transaction locks the row, sees `AVAILABLE`, sets `HELD`, commits.
2. Request B's identical query **blocks at the database level** until A's
   transaction commits.
3. Once B's lock is granted, it re-reads the row — now `HELD` — and is
   correctly rejected with a 409.

A `@Version` column on `ShowSeat` is a secondary optimistic-lock guard in case
any code path ever touches the row outside the pessimistic-lock method.

## Waitlist Auto-Assignment & Time-Limited Offers
- `POST /api/waitlist/join` adds a `Waitlist` row (`status=WAITING`) for a
  sold-out (show, category).
- On booking cancellation, `WaitlistService.offerNextInLine()` is called per
  freed seat: it locks the seat, pops the earliest `WAITING` entry (FIFO by
  `joinedAt`), sets the seat to `HELD`, creates a `WaitlistOffer` with its own
  TTL (`WAITLIST_OFFER_TTL_MINUTES`, default 15) and a random `offerToken`,
  and emails a link `{FRONTEND_BASE_URL}/waitlist-offer/{token}`.
- `GET /api/waitlist/offer/{token}` lets the customer preview the offer;
  `POST /api/waitlist/offer/{token}/accept` (authenticated) completes the booking.
- A second scheduled job (`WaitlistOfferCleanupScheduler`) expires any offer
  past its TTL and **immediately cascades** — calls `offerNextInLine()` again
  for the same seat — so it moves to the next person in line, or is released
  to general sale if the queue is empty.

## API Overview
| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | — | returns JWT |
| POST | `/api/venues` | ADMIN | create venue + categories + seats |
| GET | `/api/venues`, `/api/venues/{id}/seats` | — | browse venues |
| POST | `/api/shows` | ORGANISER/ADMIN | create show, prices, materializes `show_seats` |
| GET | `/api/shows`, `/api/shows/{id}` | — | browse/filter events |
| GET | `/api/shows/{id}/summary` | ORGANISER/ADMIN | bookings & revenue |
| GET | `/api/shows/{id}/seats` | — | seat map with live status |
| POST | `/api/shows/{id}/seats/hold` | CUSTOMER | hold a seat (TTL) |
| POST | `/api/shows/{id}/seats/release` | CUSTOMER | manually release |
| POST | `/api/shows/{id}/seats/confirm` | CUSTOMER | confirm booking from held seats → email+QR |
| GET | `/api/bookings/me` | CUSTOMER | booking history |
| POST | `/api/bookings/{id}/cancel` | CUSTOMER | cancel → triggers waitlist offer |
| POST | `/api/waitlist/join` | CUSTOMER | join queue for sold-out category |
| GET/POST | `/api/waitlist/offer/{token}[/accept]` | — / CUSTOMER | view/accept a time-limited offer |

## QR Code & Email
- `QrCodeService` encodes just the `bookingReference` string into a PNG QR
  (ZXing) — no booking details are embedded in the code itself; a scanner
  looks the reference up server-side.
- `EmailService` sends the confirmation with the QR PNG attached. Email
  failures are logged but **never fail the booking transaction** — the
  customer can always retrieve the ticket from booking history.

## Known Limitations / What Was Cut for Time
- Seat map updates via polling (4s), not WebSockets — simpler to reason about
  and sufficient for the concurrency guarantees that are actually evaluated.
- No payment gateway integration (out of scope per the brief).
- Waitlist offers currently target one seat per offer, matching "seat is
  offered to the next customer" in the brief; multi-seat waitlist joins would
  need an extension to hold multiple seats atomically.
