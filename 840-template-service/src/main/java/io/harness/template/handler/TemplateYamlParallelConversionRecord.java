package io.harness.template.handler;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateYamlParallelConversionRecord implements TemplateYamlConversionRecord {
  String path;
  // In case path corresponds to an array, fieldsToAdd will be added as single array element.
  Map<String, Object> fieldsToAdd;

  @Override
  public FieldPlacementStrategy getFieldPlacementStrategy() {
    return FieldPlacementStrategy.PARALLEL;
  }
}
