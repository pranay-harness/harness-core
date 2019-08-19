package software.wings.service.impl.deployment.checks;

import lombok.Value;

import java.util.List;

/**
 * Deployment Context POJO.
 */
@Value
public class DeploymentCtx {
  // a deployment can be associated with only one appIds
  private String appId;

  // a pipeline deployment can be associated with multiple envIds
  private List<String> envIds;
}
