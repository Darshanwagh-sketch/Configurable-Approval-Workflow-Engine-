package com.example.workflow.mapper;

import com.example.workflow.dto.ApprovalHistoryDto;
import com.example.workflow.dto.RequestResponseDto;
import com.example.workflow.entity.ApprovalHistory;
import com.example.workflow.entity.Request;
import com.example.workflow.enums.HistoryAction;
import com.example.workflow.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DtoMapperTest {

    private DtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DtoMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("Mapper: Handles null entities safely")
    void testNullEntities() {
        assertNull(mapper.toRequestResponseDto(null, null));
        assertNull(mapper.toApprovalHistoryDto(null));
        assertNull(mapper.serializePayload(null));
        assertNull(mapper.serializePayload(Map.of()));
    }

    @Test
    @DisplayName("Mapper: Correctly transforms Request to RequestResponseDto with structured JSON payload")
    void testRequestToDtoWithPayload() {
        Request request = new Request("EXPENSE", "john", "{\"amount\": 500, \"currency\": \"USD\"}", 1);
        request.setId(10L);

        RequestResponseDto dto = mapper.toRequestResponseDto(request, Role.APPROVER);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("EXPENSE", dto.getType());
        assertEquals("john", dto.getCreatedBy());
        assertEquals(Role.APPROVER, dto.getCurrentStepRole());
        assertNotNull(dto.getPayload());
        assertEquals(500, dto.getPayload().get("amount"));
    }

    @Test
    @DisplayName("Mapper: Correctly transforms non-JSON payload into raw map entry")
    void testRequestToDtoWithNonJsonPayload() {
        Request request = new Request("LEAVE", "john", "Plain text message", 1);
        request.setId(20L);

        RequestResponseDto dto = mapper.toRequestResponseDto(request, null);

        assertNotNull(dto);
        assertEquals("Plain text message", dto.getPayload().get("raw"));
    }

    @Test
    @DisplayName("Mapper: Transforms ApprovalHistory to ApprovalHistoryDto")
    void testHistoryMapping() {
        ApprovalHistory history = new ApprovalHistory(5L, HistoryAction.APPROVED, "manager1", 1, "Approved promptly");
        history.setId(100L);

        ApprovalHistoryDto dto = mapper.toApprovalHistoryDto(history);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(5L, dto.getRequestId());
        assertEquals(HistoryAction.APPROVED, dto.getAction());
        assertEquals("manager1", dto.getActionBy());
        assertEquals(1, dto.getStepOrder());
        assertEquals("Approved promptly", dto.getRemarks());
    }
}
