package com.example.workflow.repository;

import com.example.workflow.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByRequestTypeAndWorkflowVersionAndActiveTrueOrderByStepOrderAsc(String requestType, Integer workflowVersion);

    Optional<ApprovalStep> findByRequestTypeAndWorkflowVersionAndStepOrderAndActiveTrue(String requestType, Integer workflowVersion, Integer stepOrder);

    Optional<ApprovalStep> findFirstByRequestTypeAndActiveTrueOrderByWorkflowVersionDesc(String requestType);

    boolean existsByRequestTypeAndActiveTrue(String requestType);
}
