package com.example.workflow.exception;

public class WorkflowTransitionException extends RuntimeException {
    public WorkflowTransitionException(String message) {
        super(message);
    }
}
