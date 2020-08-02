package io.harness.gitsync.common.dao.api.repositories.yamlGitConfig;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.EntityScope.Scope;
import io.harness.gitsync.common.beans.YamlGitConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlGitConfigRepository extends CrudRepository<YamlGitConfig, String> {
  Long removeByAccountIdAndOrganizationIdAndProjectIdAndScopeAndUuid(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope, String uuid);

  List<YamlGitConfig> getByAccountIdAndOrganizationIdAndProjectIdAndScope(
      String accountId, String organizationIdentifier, String projectIdentifier, Scope scope);
}
