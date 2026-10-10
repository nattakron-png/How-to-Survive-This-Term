
package com.example.tournament.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // ค้นหา Player ทั้งหมดใน Team
    Page<Player> findByTeamId(Long teamId, Pageable pageable);

    // ค้นหา Player ใน Team ตามชื่อ
    Page<Player> findByTeamIdAndNameContainingIgnoreCase(
            Long teamId,
            String name,
            Pageable pageable);

    // ค้นหา Player ใน Team ตาม Role
    Page<Player> findByTeamIdAndRoleIgnoreCase(
            Long teamId,
            String role,
            Pageable pageable);

    // ค้นหา Player ใน Team ตามทั้งชื่อและ Role
    Page<Player> findByTeamIdAndNameContainingIgnoreCaseAndRoleIgnoreCase(
            Long teamId,
            String name,
            String role,
            Pageable pageable);

    // นับจำนวน Player ใน Team
    long countByTeamId(Long teamId);

    // ค้นหา Player จากชื่อ โดยไม่จำกัด Team
    Page<Player> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable);
}