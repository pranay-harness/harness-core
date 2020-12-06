package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BugsnagTab {
  private String tabName;
  private String key;
  private Object value;
}
