package io.harness.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitOpsProviderCustomRepository {
  GitOpsProvider get(
      String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier);
  boolean delete(String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier);
  Page<GitOpsProvider> findAll(
      Criteria criteria, Pageable pageable, String projectIdentifier, String orgIdentifier, String accountIdentifier);
  GitOpsProvider save(GitOpsProvider gitopsProvider);
  GitOpsProvider update(GitOpsProvider gitopsProvider);
}
