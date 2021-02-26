package io.harness.pms.sample.cd.beans;

import io.harness.pms.yaml.ParameterField;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InfrastructureDefinition {
  String uuid;
  String type;
  ParameterField<Boolean> tmpBool;
  Map<String, Object> spec;
}
