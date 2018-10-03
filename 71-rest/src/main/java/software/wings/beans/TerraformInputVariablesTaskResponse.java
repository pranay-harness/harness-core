package software.wings.beans;

import lombok.Builder;
import lombok.Value;
import software.wings.api.TerraformExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;

@Builder
@Value
public class TerraformInputVariablesTaskResponse implements NotifyResponseData {
  private final List<NameValuePair> variablesList;
  private final TerraformExecutionData terraformExecutionData;
}
