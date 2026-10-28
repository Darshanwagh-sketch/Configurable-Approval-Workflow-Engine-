package com.example.workflow.controller;

import com.example.workflow.dto.AdminOverrideDto;
import com.example.workflow.dto.ErrorResponse;
import com.example.workflow.dto.RequestResponseDto;
import com.example.workflow.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/requests")
@Tag(name = "Administrator Workflow Control", description = "Privileged administrator operations for overriding workflows")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminWorkflowController {

    private final RequestService requestService;

    public AdminWorkflowController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/{id}/override")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Force override workflow state",
            description = "ADMIN only. Explicitly overrides a request to APPROVED or REJECTED regardless of current workflow stage. Preserves prior history and records an immutable ADMIN_OVERRIDE audit event.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workflow state successfully overridden",
                            content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid override payload or missing remarks",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden (non-ADMIN)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Request not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Conflict: Request already in target status",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RequestResponseDto> overrideRequest(
            @PathVariable Long id,
            @Valid @RequestBody AdminOverrideDto dto
    ) {
        return ResponseEntity.ok(requestService.adminOverride(id, dto));
    }
}
