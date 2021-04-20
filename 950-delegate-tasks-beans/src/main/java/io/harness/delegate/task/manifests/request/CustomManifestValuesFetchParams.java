package io.harness.delegate.task.manifests.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.Cd1ApplicationAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomManifestValuesFetchParams implements TaskParameters, ActivityAccess, Cd1ApplicationAccess {
  @Expression(ALLOW_SECRETS) private List<CustomManifestFetchConfig> fetchFilesList;
  private String activityId;
  private String commandUnitName;
  private String accountId;
  private String appId;
}
