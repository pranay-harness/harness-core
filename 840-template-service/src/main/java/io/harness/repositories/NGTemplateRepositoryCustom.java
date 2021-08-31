package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;

import java.util.Optional;

@OwnedBy(CDC)
public interface NGTemplateRepositoryCustom {
  TemplateEntity save(TemplateEntity templateToSave, NGTemplateConfig templateConfig);

  Optional<TemplateEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier, String versionLabel,
      boolean notDeleted);

  TemplateEntity updateTemplateYaml(
      TemplateEntity templateEntity, NGTemplateConfig templateConfig, ChangeType changeType);
}
