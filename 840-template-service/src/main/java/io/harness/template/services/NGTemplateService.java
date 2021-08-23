package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.template.entity.TemplateEntity;

@OwnedBy(CDC)
public interface NGTemplateService {
  TemplateEntity create(TemplateEntity templateEntity);

  TemplateEntity updateTemplateEntity(TemplateEntity templateEntity, ChangeType changeType);
}
