package io.harness.pms.sample.cd.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class InfrastructureDefinition {
  String uuid;
  String type;
  Map<String, Object> spec;
}
