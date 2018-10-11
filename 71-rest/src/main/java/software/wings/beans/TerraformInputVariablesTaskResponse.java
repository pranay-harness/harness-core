package software.wings.beans;

import io.harness.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Value;
import software.wings.api.TerraformExecutionData;

import java.util.List;

@Builder
@Value
public class TerraformInputVariablesTaskResponse implements ResponseData {
  private final List<NameValuePair> variablesList;
  private final TerraformExecutionData terraformExecutionData;
}
