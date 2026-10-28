package com.example.workflow.repository;

import com.example.workflow.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    List<Request> findAllByOrderByCreatedAtDesc();
}
