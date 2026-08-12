package com.smartdoc.search;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SearchSqlBoundsTest {
    @Test
    void everyUnifiedSearchQueryUsesEscapedLiteralPredicateAndBoundedParameters() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        new SearchService(jdbc).search(7L, "%_\\'quoted", null, 30);

        List<Invocation> queries = mockingDetails(jdbc).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("query")).collect(Collectors.toList());
        assertFalse(queries.isEmpty());
        for (Invocation query : queries) {
            String sql = (String) query.getArgument(0);
            assertTrue(sql.toLowerCase().contains(" like "), sql);
            assertTrue(sql.toLowerCase().contains(" escape '!'"), sql);
            assertTrue(sql.toLowerCase().contains(" limit ?"), sql);
            List<Object> parameters = java.util.Arrays.asList(query.getArguments()).subList(2, query.getArguments().length);
            assertTrue(parameters.contains("%!%!_\\'quoted%"), parameters.toString());
            assertTrue(parameters.contains(30), parameters.toString());
        }
    }
}
