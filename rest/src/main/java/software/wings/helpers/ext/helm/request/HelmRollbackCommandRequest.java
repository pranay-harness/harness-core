package software.wings.helpers.ext.helm.request;

import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ContainerServiceParams;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
public class HelmRollbackCommandRequest extends HelmCommandRequest {
  private String revision;
  private String releaseName;

  public HelmRollbackCommandRequest() {
    super(HelmCommandType.ROLLBACK);
  }

  @Builder
  public HelmRollbackCommandRequest(String accountId, String appId, String kubeConfigLocation, String commandName,
      String activityId, ContainerServiceParams containerServiceParams, String revision, String releaseName) {
    super(HelmCommandType.ROLLBACK, accountId, appId, kubeConfigLocation, commandName, activityId,
        containerServiceParams);
    this.revision = revision;
    this.releaseName = releaseName;
  }
}
