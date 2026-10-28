package com.example.workflow.config;

import com.example.workflow.entity.ApprovalStep;
import com.example.workflow.entity.User;
import com.example.workflow.enums.Role;
import com.example.workflow.repository.ApprovalStepRepository;
import com.example.workflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            UserRepository userRepository,
            ApprovalStepRepository approvalStepRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedWorkflowConfigurations();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding initial development users with secure BCrypt passwords...");

            User requester1 = new User("requester1", passwordEncoder.encode("password123"), Role.REQUESTER);
            User requester2 = new User("requester2", passwordEncoder.encode("password123"), Role.REQUESTER);
            User approver1 = new User("approver1", passwordEncoder.encode("password123"), Role.APPROVER);
            User approver2 = new User("approver2", passwordEncoder.encode("password123"), Role.APPROVER);
            User admin = new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN);

            userRepository.saveAll(List.of(requester1, requester2, approver1, approver2, admin));
            log.info("Default users seeded successfully (requester1, requester2, approver1, approver2, admin).");
        }
    }

    private void seedWorkflowConfigurations() {
        if (approvalStepRepository.count() == 0) {
            log.info("Seeding dynamic workflow step configurations for LEAVE, EXPENSE, PURCHASE_ORDER, and QUICK_REQUEST...");

            // LEAVE workflow: 2 steps (Step 1: APPROVER, Step 2: ADMIN)
            ApprovalStep leaveStep1 = new ApprovalStep("LEAVE", 1, Role.APPROVER, 1, true);
            ApprovalStep leaveStep2 = new ApprovalStep("LEAVE", 2, Role.ADMIN, 1, true);

            // EXPENSE workflow: 2 steps (Step 1: APPROVER, Step 2: ADMIN)
            ApprovalStep expenseStep1 = new ApprovalStep("EXPENSE", 1, Role.APPROVER, 1, true);
            ApprovalStep expenseStep2 = new ApprovalStep("EXPENSE", 2, Role.ADMIN, 1, true);

            // PURCHASE_ORDER workflow: 3 steps (Step 1: APPROVER, Step 2: APPROVER, Step 3: ADMIN)
            ApprovalStep poStep1 = new ApprovalStep("PURCHASE_ORDER", 1, Role.APPROVER, 1, true);
            ApprovalStep poStep2 = new ApprovalStep("PURCHASE_ORDER", 2, Role.APPROVER, 1, true);
            ApprovalStep poStep3 = new ApprovalStep("PURCHASE_ORDER", 3, Role.ADMIN, 1, true);

            // QUICK_REQUEST workflow: 1 step (Step 1: APPROVER)
            ApprovalStep quickStep1 = new ApprovalStep("QUICK_REQUEST", 1, Role.APPROVER, 1, true);

            approvalStepRepository.saveAll(List.of(
                    leaveStep1, leaveStep2,
                    expenseStep1, expenseStep2,
                    poStep1, poStep2, poStep3,
                    quickStep1
            ));
            log.info("Dynamic workflow configurations successfully seeded into the database.");
        }
    }
}
