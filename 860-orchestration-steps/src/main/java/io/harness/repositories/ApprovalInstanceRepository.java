package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.approval.step.entities.ApprovalInstance;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@OwnedBy(HarnessTeam.CDC)
@HarnessRepo
@Transactional
public interface ApprovalInstanceRepository
    extends CrudRepository<ApprovalInstance, String>, ApprovalInstanceCustomRepository {}
