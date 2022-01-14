package io.harness.template.handler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateYamlReplaceConversionRecord implements TemplateYamlConversionRecord {
  String path;
  Object fieldsToAdd;

  @Override
  public FieldPlacementStrategy getFieldPlacementStrategy() {
    return FieldPlacementStrategy.REPLACE;
  }
}
