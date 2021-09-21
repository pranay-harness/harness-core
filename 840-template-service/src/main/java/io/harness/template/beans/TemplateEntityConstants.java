package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface TemplateEntityConstants {
  String STEP = "Step";
  String STABLE_TEMPLATE = "Stable";
  String LAST_UPDATES_TEMPLATE = "LastUpdated";
  String ALL = "All";
  String TEMPLATE_STABLE_TRUE_WITH_YAML_CHANGE = "TemplateStableTrueWithYamlChange";
  String TEMPLATE_STABLE_TRUE = "TemplateStableTrue";
  String TEMPLATE_STABLE_FALSE = "TemplateStableFalse";
  String TEMPLATE_LAST_UPDATED_FALSE = "TemplateLastUpdatedFalse";
  String TEMPLATE_LAST_UPDATED_TRUE = "TemplateLastUpdatedTrue";
  String TEMPLATE_CHANGE_SCOPE = "TemplateChangeScope";
  String OTHERS = "Others";
}
