package com.example.tournament.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentStatus;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    // ค้นหาทัวร์นาเมนต์จากชื่อบางส่วน โดยไม่สนใจตัวพิมพ์ใหญ่หรือเล็ก
    List<Tournament> findByNameContainingIgnoreCase(String name);

    // ค้นหาทัวร์นาเมนต์ตามสถานะ
    List<Tournament> findByStatus(TournamentStatus status);

    // ค้นหาทัวร์นาเมนต์จากชื่อบางส่วนและสถานะพร้อมกัน
    List<Tournament> findByNameContainingIgnoreCaseAndStatus(
            String name,
            TournamentStatus status);

    Optional<Tournament> findByName(String name);

    boolean existsByName(String name);
}
