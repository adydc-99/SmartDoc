package com.smartdoc.search;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchDatabaseMatchSemanticsTest {
    @Test
    void boundedSqlCandidatesAreNeverDiscardedByDifferentJavaCollationRules() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> mapper, Object... arguments) {
            if (!sql.startsWith("SELECT id,name")) return Collections.emptyList();
            List<T> rows = new ArrayList<>();
            for (int index = 0; index < 30; index++) {
                ResultSet rs = databaseSelectedRow(index);
                try {
                    rows.add(mapper.mapRow(rs, index));
                } catch (java.sql.SQLException error) {
                    throw new IllegalStateException(error);
                }
            }
            return rows;
            }
        };

        List<UnifiedSearchHit> hits = new SearchService(jdbc).search(5L, "cafe", "document", 30);

        assertEquals(30, hits.size(), "SQL's bounded result must be the authoritative candidate set");
        assertEquals("Bounded café candidate 29", hits.get(0).getTitle());
    }

    private static ResultSet databaseSelectedRow(int index) {
        try {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("id")).thenReturn((long) index + 1);
            when(rs.getString("name")).thenReturn("Bounded café candidate " + index);
            when(rs.getString("content_text")).thenReturn(null);
            when(rs.getString("match_prefix")).thenReturn("Bounded ");
            when(rs.getInt("match_rank")).thenReturn(0);
            when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(index)));
            return rs;
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException(error);
        }
    }
}
