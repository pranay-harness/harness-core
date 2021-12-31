package io.harness.template.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.pms.yaml.YamlField;

@OwnedBy(HarnessTeam.CDC)
public interface YamlConversionHandler {
  String getRootField(TemplateEntityType templateEntityType);
  TemplateYamlConversionData getAdditionalFieldsToAdd(TemplateEntityType templateEntityType, YamlField yamlField);
}
