package software.wings.cdng.common.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Tag {
  private String key;
  private String value;
}
