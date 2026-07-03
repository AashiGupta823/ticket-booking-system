package com.tbs.dto;

public class WaitlistDtos {

    public record JoinWaitlistRequest(Long showId, Long categoryId) {}
}
