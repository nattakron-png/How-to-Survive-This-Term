package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TournamentExistsRuleTest {

    @Test
    void shouldPassWhenTournamentExists() {
        TournamentRepository repo = mock(TournamentRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getId()).thenReturn(20L);
        when(repo.existsById(20L)).thenReturn(true);

        TournamentExistsRule rule = new TournamentExistsRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenTournamentDoesNotExist() {
        TournamentRepository repo = mock(TournamentRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getId()).thenReturn(20L);
        when(repo.existsById(20L)).thenReturn(false);

        TournamentExistsRule rule = new TournamentExistsRule(repo);

        assertThrows(
            ResourceNotFoundException.class,
            () -> rule.validate(team, tournament)
        );
    }
}
