package io.harness.template.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.services.NGTemplateService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDC)
public class TemplateCRUDHelper {
  @Inject private NGTemplateService templateService;

  public TemplateEntity create(TemplateEntity templateEntity, boolean setDefaultTemplate, String comments) {
    return templateService.create(templateEntity, setDefaultTemplate, comments);
  }

  public TemplateEntity updateTemplateEntity(
      TemplateEntity templateEntity, ChangeType changeType, boolean setDefaultTemplate, String comments) {
    return templateService.updateTemplateEntity(templateEntity, changeType, setDefaultTemplate, comments);
  }
}
