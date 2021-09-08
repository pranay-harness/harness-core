package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(CDC)
public interface NGTemplateRepositoryCustom {
  TemplateEntity save(TemplateEntity templateToSave, NGTemplateConfig templateConfig);

  Optional<TemplateEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier, String versionLabel,
      boolean notDeleted);

  Optional<TemplateEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndIsStableAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier, boolean notDeleted);

  TemplateEntity updateTemplateYaml(TemplateEntity templateEntity, TemplateEntity oldTemplateEntity,
      NGTemplateConfig templateConfig, ChangeType changeType);

  TemplateEntity deleteTemplate(TemplateEntity templateToDelete, NGTemplateConfig templateConfig);

  Page<TemplateEntity> findAll(Criteria criteria, Pageable pageable, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, boolean getDistinctFromBranches);
}
