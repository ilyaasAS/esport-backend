package org.example.web.dto;

import java.time.LocalDate;

public record MatchDTO(
        int id,
        int player1Id,
        String player1Nickname,
        int player2Id,
        String player2Nickname,
        int scorePlayer1,
        int scorePlayer2,
        LocalDate date
) {
}
