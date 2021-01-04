package io.harness.pms.sample.cd.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Service {
  String uuid;
  String identifier;
  String name;
  ServiceDefinition serviceDefinition;
}
