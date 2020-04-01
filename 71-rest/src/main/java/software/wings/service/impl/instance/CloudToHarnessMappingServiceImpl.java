package software.wings.service.impl.instance;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentSummary;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.service.intfc.instance.DeploymentService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class CloudToHarnessMappingServiceImpl implements CloudToHarnessMappingService {
  private final HPersistence persistence;
  private final DeploymentService deploymentService;

  @Inject
  public CloudToHarnessMappingServiceImpl(HPersistence persistence, DeploymentService deploymentService) {
    this.persistence = persistence;
    this.deploymentService = deploymentService;
  }

  @Override
  public Optional<HarnessServiceInfo> getHarnessServiceInfo(DeploymentSummary deploymentSummary) {
    Optional<DeploymentSummary> summary = deploymentService.getWithAccountId(deploymentSummary);
    if (summary.isPresent()) {
      DeploymentSummary deploymentSummaryResponse = summary.get();
      InfrastructureMapping infrastructureMapping =
          persistence.createQuery(InfrastructureMapping.class)
              .filter(InfrastructureMappingKeys.uuid, deploymentSummaryResponse.getInfraMappingId())
              .get();

      if (infrastructureMapping != null) {
        return Optional.of(
            new HarnessServiceInfo(infrastructureMapping.getServiceId(), infrastructureMapping.getAppId(),
                infrastructureMapping.getComputeProviderSettingId(), infrastructureMapping.getEnvId(),
                deploymentSummaryResponse.getInfraMappingId(), deploymentSummaryResponse.getUuid()));
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<SettingAttribute> getSettingAttribute(String id) {
    return Optional.ofNullable(
        persistence.createQuery(SettingAttribute.class).filter(SettingAttributeKeys.uuid, id).get());
  }

  @Override
  public List<HarnessServiceInfo> getHarnessServiceInfoList(List<DeploymentSummary> deploymentSummaryList) {
    List<String> infraMappingIds =
        deploymentSummaryList.stream().map(DeploymentSummary::getInfraMappingId).collect(Collectors.toList());
    List<InfrastructureMapping> infrastructureMappings =
        persistence.createQuery(InfrastructureMapping.class, excludeAuthority)
            .field(InfrastructureMappingKeys.uuid)
            .in(infraMappingIds)
            .asList();
    return infrastructureMappings.stream()
        .map(infrastructureMapping
            -> new HarnessServiceInfo(infrastructureMapping.getServiceId(), infrastructureMapping.getAppId(),
                infrastructureMapping.getComputeProviderSettingId(), infrastructureMapping.getEnvId(),
                infrastructureMapping.getUuid(), null))
        .collect(Collectors.toList());
  }

  @Override
  public List<Account> getCCMEnabledAccounts() {
    List<Account> accounts = new ArrayList<>();
    try (HIterator<Account> query = new HIterator<>(persistence.createQuery(Account.class, excludeAuthority)
                                                        .filter(AccountKeys.cloudCostEnabled, Boolean.TRUE)
                                                        .fetch())) {
      for (Account account : query) {
        accounts.add(account);
      }
    }
    return accounts;
  }

  @Override
  public List<ResourceLookup> getResourceList(String accountId, List<String> resourceIds) {
    return persistence.createQuery(ResourceLookup.class)
        .filter(ResourceLookupKeys.accountId, accountId)
        .field(ResourceLookupKeys.resourceId)
        .in(resourceIds)
        .asList();
  }

  @Override
  public List<DeploymentSummary> getDeploymentSummary(
      String accountId, String offset, Instant startTime, Instant endTime) {
    return deploymentService.getDeploymentSummary(accountId, offset, startTime, endTime);
  }

  @Override
  public List<SettingAttribute> getSettingAttributes(String accountId, String category, String valueType) {
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    try (HIterator<SettingAttribute> query =
             new HIterator<>(persistence.createQuery(SettingAttribute.class, excludeAuthority)
                                 .filter(SettingAttributeKeys.accountId, accountId)
                                 .filter(SettingAttributeKeys.category, category)
                                 .filter(SettingAttributeKeys.valueType, valueType)
                                 .fetch())) {
      for (SettingAttribute settingAttribute : query) {
        settingAttributes.add(settingAttribute);
      }
    }
    return settingAttributes;
  }
}
