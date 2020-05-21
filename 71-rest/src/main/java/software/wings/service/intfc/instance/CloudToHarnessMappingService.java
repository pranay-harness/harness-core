package software.wings.service.intfc.instance;

import io.harness.ccm.config.GcpBillingAccount;
import software.wings.api.DeploymentSummary;
import software.wings.beans.Account;
import software.wings.beans.ResourceLookup;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.settings.SettingValue.SettingVariableTypes;

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

  Account getAccountInfoFromId(String accountId);

  List<ResourceLookup> getResourceList(String accountId, List<String> resourceIds);

  List<DeploymentSummary> getDeploymentSummary(String accountId, String offset, Instant startTime, Instant endTime);

  List<SettingAttribute> listSettingAttributesCreatedInDuration(
      String accountId, SettingCategory category, SettingVariableTypes valueType);

  List<SettingAttribute> listSettingAttributesCreatedInDuration(
      String accountId, SettingCategory category, SettingVariableTypes valueType, long startTime, long endTime);

  List<GcpBillingAccount> listGcpBillingAccountUpdatedInDuration(String accountId, long startTime, long endTime);

  String getEntityName(BillingDataQueryMetadata.BillingDataMetaDataFields field, String entityId);
}
