package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvSummary {
  private String name;
  private String uuid;
  private EnvironmentType environmentType;
}
