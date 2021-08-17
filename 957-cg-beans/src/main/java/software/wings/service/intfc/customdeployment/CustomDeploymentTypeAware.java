package software.wings.service.intfc.customdeployment;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface CustomDeploymentTypeAware {
  String getDeploymentTypeTemplateId();
  void setDeploymentTypeName(String theCustomDeploymentName);
}
