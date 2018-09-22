package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExistingStackInfo {
  private boolean stackExisted;
  private String oldStackBody;
  private Map<String, String> oldStackParameters;
}