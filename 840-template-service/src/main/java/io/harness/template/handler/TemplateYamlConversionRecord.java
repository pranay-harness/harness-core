package io.harness.template.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public interface TemplateYamlConversionRecord {
  FieldPlacementStrategy getFieldPlacementStrategy();
  String getPath();
  Object getFieldsToAdd();
}
