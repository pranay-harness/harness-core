package software.wings.helpers.ext.cloudformation.response;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExistingStackInfo {
  private boolean stackExisted;
  private String oldStackBody;
  private Map<String, String> oldStackParameters;
}
