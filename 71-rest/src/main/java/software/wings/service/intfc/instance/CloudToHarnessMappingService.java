package software.wings.service.intfc.instance;

import software.wings.api.DeploymentSummary;
import software.wings.beans.Account;
import software.wings.beans.ResourceLookup;
import software.wings.beans.SettingAttribute;
import software.wings.beans.instance.HarnessServiceInfo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CloudToHarnessMappingService {
  Optional<HarnessServiceInfo> getHarnessServiceInfo(DeploymentSummary deploymentSummary);

  Optional<HarnessServiceInfo> getHarnessServiceInfo(
      String accountId, String computeProviderId, String namespace, String podName);

  Optional<SettingAttribute> getSettingAttribute(String id);

  List<HarnessServiceInfo> getHarnessServiceInfoList(List<DeploymentSummary> deploymentSummaryList);

  List<Account> getCCMEnabledAccounts();

  String getAccountNameFromId(String accountId);

  List<ResourceLookup> getResourceList(String accountId, List<String> resourceIds);

  List<DeploymentSummary> getDeploymentSummary(String accountId, String offset, Instant startTime, Instant endTime);

  List<SettingAttribute> getSettingAttributes(String accountId, String category, String valueType);

  List<SettingAttribute> getSettingAttributes(
      String accountId, String category, String valueType, long startTime, long endTime);
}
