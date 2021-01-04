package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureTagDetails {
  private String tagName;
  private List<String> values;
}
