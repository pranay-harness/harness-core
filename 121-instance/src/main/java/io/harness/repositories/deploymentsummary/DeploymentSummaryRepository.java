package io.harness.repositories.deploymentsummary;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;

import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface DeploymentSummaryRepository
    extends CrudRepository<DeploymentSummary, String>, DeploymentSummaryCustom {}
