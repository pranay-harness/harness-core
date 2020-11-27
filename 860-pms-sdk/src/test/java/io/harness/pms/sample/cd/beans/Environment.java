package io.harness.pms.sample.cd.beans;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Environment {
  String uuid;
  String type;
  String identifier;
  String name;
}
