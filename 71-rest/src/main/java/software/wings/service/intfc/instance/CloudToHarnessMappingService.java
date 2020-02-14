package software.wings.service.intfc.instance;

import software.wings.api.DeploymentSummary;
import software.wings.beans.Account;
import software.wings.beans.ResourceLookup;
import software.wings.beans.SettingAttribute;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.List;
import java.util.Optional;

public interface CloudToHarnessMappingService {
  Optional<HarnessServiceInfo> getHarnessServiceInfo(DeploymentSummary deploymentSummary);

  Optional<SettingAttribute> getSettingAttribute(String id);

  List<HarnessServiceInfo> getHarnessServiceInfoList(List<DeploymentSummary> deploymentSummaryList);

  List<Account> getCCMEnabledAccounts();

  List<ResourceLookup> getResourceList(String accountId, List<String> resourceIds);
}
