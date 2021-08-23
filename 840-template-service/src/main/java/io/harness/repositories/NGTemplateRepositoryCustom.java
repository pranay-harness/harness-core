package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;

@OwnedBy(CDC)
public interface NGTemplateRepositoryCustom {
  TemplateEntity save(TemplateEntity templateToSave, NGTemplateConfig templateConfig);
}
