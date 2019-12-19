package software.wings.service.intfc.instance;

import software.wings.api.DeploymentSummary;
import software.wings.beans.SettingAttribute;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.Optional;

public interface CloudToHarnessMappingService {
  Optional<HarnessServiceInfo> getHarnessServiceInfo(DeploymentSummary deploymentSummary);

  Optional<SettingAttribute> getSettingAttribute(String id);
}
