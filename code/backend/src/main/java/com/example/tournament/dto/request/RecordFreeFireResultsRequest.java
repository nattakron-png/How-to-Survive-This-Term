package com.example.tournament.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record RecordFreeFireResultsRequest(
        @NotEmpty List<@Valid FreeFireTeamResultRequest> results) {
}