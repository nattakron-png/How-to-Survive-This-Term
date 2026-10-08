package com.example.tournament.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // ค้นหา Player ทั้งหมดใน Team
    Page<Player> findByTeamId(Long teamId, Pageable pageable);

    // ค้นหา Player ใน Team ตามชื่อ
    // Containing = ค้นหาคำที่อยู่ภายในชื่อ
    // IgnoreCase = ไม่สนใจตัวพิมพ์ใหญ่/เล็ก
    Page<Player> findByTeamIdAndNameContainingIgnoreCase(
            Long teamId,
            String name,
            Pageable pageable);
}