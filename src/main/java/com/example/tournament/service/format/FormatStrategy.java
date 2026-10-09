package com.example.tournament.service.format;

import java.util.List;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentFormat;

public interface FormatStrategy {

    TournamentFormat format();

    /** สร้างตารางการแข่งและบันทึกลงฐานข้อมูล คืนจำนวนแมตช์หรือเกมที่สร้าง */
    int createSchedule(Tournament tournament, List<Team> teamsInSeedOrder);
}
