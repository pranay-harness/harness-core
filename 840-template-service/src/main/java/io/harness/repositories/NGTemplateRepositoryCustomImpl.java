package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(CDC)
public class NGTemplateRepositoryCustomImpl implements NGTemplateRepositoryCustom {
  private final GitAwarePersistence gitAwarePersistence;

  @Override
  public TemplateEntity save(TemplateEntity templateToSave, NGTemplateConfig templateConfig) {
    return gitAwarePersistence.save(templateToSave, templateToSave.getYaml(), ChangeType.ADD, TemplateEntity.class);
  }
}
