package com.agent2026.interview.controller;

import com.agent2026.interview.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db")
    public Result<Map<String, String>> database() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("database", "mysql");
        data.put("version", version);
        return Result.success(data);
    }
}
