
package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;

public interface TeamJoinRule {

    void validate(Team team, Tournament tournament);

    void setNext(TeamJoinRule nextRule);
}