package com.example.workflow.entity;

import com.example.workflow.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "approval_steps", uniqueConstraints = {
    @UniqueConstraint(name = "uk_step_workflow_order", columnNames = {"request_type", "workflow_version", "step_order"})
}, indexes = {
    @Index(name = "idx_step_type_version", columnList = "request_type, workflow_version")
})
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_type", nullable = false, length = 50)
    private String requestType;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion = 1;

    @Column(nullable = false)
    private boolean active = true;

    public ApprovalStep() {
    }

    public ApprovalStep(String requestType, Integer stepOrder, Role role) {
        this(requestType, stepOrder, role, 1, true);
    }

    public ApprovalStep(String requestType, Integer stepOrder, Role role, Integer workflowVersion, boolean active) {
        this.requestType = requestType;
        this.stepOrder = stepOrder;
        this.role = role;
        this.workflowVersion = workflowVersion != null ? workflowVersion : 1;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(Integer workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
