package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.git.model.ChangeType;
import io.harness.template.entity.TemplateEntity;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(CDC)
public interface NGTemplateService {
  TemplateEntity create(TemplateEntity templateEntity, boolean setDefaultTemplate, String comments);

  TemplateEntity updateTemplateEntity(
      TemplateEntity templateEntity, ChangeType changeType, boolean setDefaultTemplate, String comments);

  Optional<TemplateEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, String versionLabel, boolean deleted);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier,
      String versionLabel, Long version, String comments);

  boolean deleteTemplates(String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier,
      Set<String> templateVersions, String comments);

  Page<TemplateEntity> list(Criteria criteria, Pageable pageable, String accountId, String orgIdentifier,
      String projectIdentifier, Boolean getDistinctFromBranches);

  TemplateEntity updateStableTemplateVersion(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, String newStableTemplateVersion);

  boolean updateTemplateSettings(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, Scope currentScope, Scope updateScope, String updateStableTemplateVersion,
      Boolean getDistinctFromBranches);
}
