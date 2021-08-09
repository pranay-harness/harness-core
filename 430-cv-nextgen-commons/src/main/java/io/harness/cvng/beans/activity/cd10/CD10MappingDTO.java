package io.harness.cvng.beans.activity.cd10;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class CD10MappingDTO {
  private String appId;
  String appName;
}
