package com.example.tournament.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Page<Player> findByTeamId(Long teamId, Pageable pageable);
}
