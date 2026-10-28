package com.example.workflow.controller;

import com.example.workflow.dto.*;
import com.example.workflow.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/requests")
@Tag(name = "Requests & Workflow", description = "Endpoints for creating, approving, rejecting, and inspecting requests")
@SecurityRequirement(name = "Bearer Authentication")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('REQUESTER')")
    @Operation(
            summary = "Create a new approval request",
            description = "Submits a request of any configured type. Validates database workflow configuration, initializes the request at step 1, and records an audit event.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Request successfully created",
                            content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid payload or unconfigured workflow type",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden (non-REQUESTER)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RequestResponseDto> createRequest(@Valid @RequestBody CreateRequestDto dto) {
        RequestResponseDto created = requestService.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'APPROVER', 'ADMIN')")
    @Operation(
            summary = "Get request details",
            description = "Retrieves request metadata, status, creator, current step, and dynamically loaded role for the current step.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request details returned",
                            content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Requesters can only access their own requests",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Request not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RequestResponseDto> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    @Operation(
            summary = "Approve request at current step",
            description = "Advances request to the next step or marks it APPROVED if at terminal step. Enforces current-step role authorization and self-approval prevention.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Step approved successfully",
                            content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Role mismatch or self-approval attempt",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Request not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Conflict: Request is not in PENDING status or concurrent modification",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RequestResponseDto> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveRequestDto dto
    ) {
        return ResponseEntity.ok(requestService.approveRequest(id, dto));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    @Operation(
            summary = "Reject request at current step",
            description = "Terminates the workflow with status REJECTED and records audit entry. Enforces current-step role authorization and self-rejection prevention.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Request rejected successfully",
                            content = @Content(schema = @Schema(implementation = RequestResponseDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Role mismatch or self-rejection attempt",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Request not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Conflict: Request is not in PENDING status",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<RequestResponseDto> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) RejectRequestDto dto
    ) {
        return ResponseEntity.ok(requestService.rejectRequest(id, dto));
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER', 'ADMIN')")
    @Operation(
            summary = "Retrieve approval audit history",
            description = "Returns the chronological, immutable audit history for the request. Ordinary requesters may only view history for their own requests.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Approval history returned chronologically",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApprovalHistoryDto.class)))),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Requesters can only access their own history",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Request not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<List<ApprovalHistoryDto>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestHistory(id));
    }
}
