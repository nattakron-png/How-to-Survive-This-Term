package com.example.tournament.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SeedAssignmentRequest(
        @NotEmpty(message = "ต้องระบุทีมที่ต้องการจัด Seed")
        List<@Valid SeedItem> assignments) {

    public record SeedItem(
            @NotNull(message = "กรุณาระบุ teamId")
            @Positive(message = "teamId ต้องมากกว่า 0")
            Long teamId,

            @NotNull(message = "กรุณาระบุ seed")
            @Positive(message = "seed ต้องมากกว่า 0")
            Integer seed) {
    }
}