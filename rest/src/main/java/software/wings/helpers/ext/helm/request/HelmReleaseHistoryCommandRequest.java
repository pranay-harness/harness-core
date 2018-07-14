package software.wings.helpers.ext.helm.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.service.impl.ContainerServiceParams;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmReleaseHistoryCommandRequest extends HelmCommandRequest {
  public HelmReleaseHistoryCommandRequest() {
    super(HelmCommandType.RELEASE_HISTORY);
  }

  @Builder
  public HelmReleaseHistoryCommandRequest(String accountId, String appId, String kubeConfigLocation, String commandName,
      String activityId, ContainerServiceParams containerServiceParams, String releaseName) {
    super(HelmCommandType.RELEASE_HISTORY, accountId, appId, kubeConfigLocation, commandName, activityId,
        containerServiceParams, releaseName, null, null);
  }
}
