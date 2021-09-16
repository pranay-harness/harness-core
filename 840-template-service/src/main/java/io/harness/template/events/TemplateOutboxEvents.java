package io.harness.template.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public class TemplateOutboxEvents {
  public static final String TEMPLATE_VERSION_CREATED = "TemplateVersionCreated";
  public static final String TEMPLATE_VERSION_UPDATED = "TemplateVersionUpdated";
  public static final String TEMPLATE_VERSION_DELETED = "TemplateVersionDeleted";
}
