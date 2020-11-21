package io.harness.gitsync.core.dao.api.repositories.YamlSuccessfulChange;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface YamlSuccessfulChangeRepository extends CrudRepository<YamlSuccessfulChange, String> {
  Optional<YamlSuccessfulChange> findByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePath(
      String accountId, String orgId, String projectId, String yamlFilePath);
}
