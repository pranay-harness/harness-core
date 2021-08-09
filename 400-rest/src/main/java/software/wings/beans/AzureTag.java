package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureTag {
  private String key;
  private String value;
}
