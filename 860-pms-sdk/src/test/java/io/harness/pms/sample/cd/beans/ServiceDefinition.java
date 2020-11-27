package io.harness.pms.sample.cd.beans;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceDefinition {
  String uuid;
  String type;
  Map<String, Object> spec;
}
