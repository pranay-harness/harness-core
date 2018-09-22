package software.wings.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureTagDetails {
  private String tagName;
  private List<String> values;
}
