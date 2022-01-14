package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import org.springframework.data.repository.Repository;

@OwnedBy(HarnessTeam.GITOPS)
@HarnessRepo
public interface GitOpsProviderRepository extends Repository<GitOpsProvider, String>, GitOpsProviderCustomRepository {}
