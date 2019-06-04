package software.wings.beans;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;
import software.wings.api.TerraformExecutionData;

import java.util.List;

@Data
@Builder
public class TerraformInputVariablesTaskResponse implements DelegateTaskNotifyResponseData {
  private final List<NameValuePair> variablesList;
  private final TerraformExecutionData terraformExecutionData;
  private DelegateMetaInfo delegateMetaInfo;
}
