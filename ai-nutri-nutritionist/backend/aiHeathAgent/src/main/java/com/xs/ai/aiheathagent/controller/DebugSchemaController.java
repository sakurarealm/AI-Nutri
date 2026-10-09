package com.xs.ai.aiheathagent.controller;

import com.xs.ai.aiheathagent.common.ApiResp;
import com.xs.ai.aiheathagent.repo.UserRepo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class DebugSchemaController extends BaseController {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DebugSchemaController(UserRepo userRepo, DataSource dataSource, JdbcTemplate jdbcTemplate) {
        super(userRepo);
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"/api/debug/schema/sysmsg", "/api/debug/schema/sysmsg/", "/api/api/debug/schema/sysmsg", "/api/api/debug/schema/sysmsg/", "/debug/schema/sysmsg", "/debug/schema/sysmsg/"})
    public ApiResp<Map<String, Object>> sysmsgMetaSchema() {
        String product = "";
        try (Connection c = dataSource.getConnection()) {
            product = String.valueOf(c.getMetaData().getDatabaseProductName());
        } catch (Exception ignored) {
        }
        Map<String, Object> out = Map.of("dbProduct", product);
        try {
            var rows = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH AS maxLen " +
                            "FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_message_meta_records' " +
                            "AND COLUMN_NAME IN ('system_message_type','explanation_code')"
            );
            return ApiResp.ok(Map.of("dbProduct", product, "columns", rows));
        } catch (Exception ignored) {
            return ApiResp.ok(out);
        }
    }

    @GetMapping({"/api/debug/schema/nutrition", "/api/debug/schema/nutrition/", "/api/api/debug/schema/nutrition", "/api/api/debug/schema/nutrition/", "/debug/schema/nutrition", "/debug/schema/nutrition/"})
    public ApiResp<Map<String, Object>> nutritionSchema() {
        String product = "";
        try (Connection c = dataSource.getConnection()) {
            product = String.valueOf(c.getMetaData().getDatabaseProductName());
        } catch (Exception ignored) {
        }

        List<String> tables = List.of("recipes", "recipe_ingredients", "recipe_steps", "food_catalogs");
        try {
            Map<String, Object> out = Map.of(
                    "dbProduct", product,
                    "tables", tables.stream().map(t -> Map.of(
                            "table", t,
                            "columns", jdbcTemplate.queryForList(
                                    "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH AS maxLen " +
                                            "FROM information_schema.COLUMNS " +
                                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                                            "ORDER BY ORDINAL_POSITION",
                                    t
                            )
                    )).toList()
            );
            return ApiResp.ok(out);
        } catch (Exception ignored) {
            return ApiResp.ok(Map.of("dbProduct", product, "tables", tables));
        }
    }
}
