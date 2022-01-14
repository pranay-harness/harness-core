package software.wings.service.impl.instance;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
final class EnvInfo {
  private String id;
  private String name;
  private String type;
}
