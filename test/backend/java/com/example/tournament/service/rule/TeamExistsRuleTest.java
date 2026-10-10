package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TeamRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamExistsRuleTest {

    @Test
    void shouldPassWhenTeamExists() {
        TeamRepository repo = mock(TeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(team.getId()).thenReturn(10L);
        when(repo.existsById(10L)).thenReturn(true);

        TeamExistsRule rule = new TeamExistsRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenTeamDoesNotExist() {
        TeamRepository repo = mock(TeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(team.getId()).thenReturn(10L);
        when(repo.existsById(10L)).thenReturn(false);

        TeamExistsRule rule = new TeamExistsRule(repo);

        assertThrows(
            ResourceNotFoundException.class,
            () -> rule.validate(team, tournament)
        );
    }
}
