package io.harness.gitsync.common.dao.api.repositories.yamlGitFolderConfig;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.EntityScope.Scope;
import io.harness.gitsync.common.beans.YamlGitFolderConfig;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlGitFolderConfigRepository extends PagingAndSortingRepository<YamlGitFolderConfig, String> {
  List<YamlGitFolderConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScope(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope);

  List<YamlGitFolderConfig> findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndYamlGitConfigId(
      String accountId, String organizationId, String projectId, Scope scope, String identifier);

  YamlGitFolderConfig findByAccountIdAndOrganizationIdAndProjectIdAndScopeAndIsDefault(
      String accountId, String organizationId, String projectId, Scope scope, boolean isDefault);
}