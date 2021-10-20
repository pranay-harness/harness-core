package io.harness.cdng.licenserestriction;

import static io.harness.data.structure.CollectionUtils.collectionToStream;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.remote.client.NGRestUtils.getResponseWithRetry;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.PipelineSetupUsageUtils;
import io.harness.cdng.usage.beans.CDLicenseUsageDTO;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.licensing.usage.beans.LicenseUsageDTO;
import io.harness.licensing.usage.beans.ReferenceDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class EnforcementValidator {
  @Inject private LicenseUsageInterface licenseUsageInterface;
  @Inject private EntitySetupUsageClient entitySetupUsageClient;
  @Inject private EnforcementClientService enforcementClientService;

  private static final int PAGE = 0;
  private static final int SIZE = 100;

  private Cache<String, Integer> newServiceCache =
      CacheBuilder.newBuilder().maximumSize(2000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  public Set<String> getActiveServices(String accountIdentifier) {
    LicenseUsageDTO licenseUsage =
        licenseUsageInterface.getLicenseUsage(accountIdentifier, ModuleType.CD, new Date().getTime());
    Set<String> services = new HashSet<>();
    if (licenseUsage instanceof CDLicenseUsageDTO) {
      CDLicenseUsageDTO cdLicenseUsage = (CDLicenseUsageDTO) licenseUsage;
      if (null != cdLicenseUsage.getActiveServices()
          && isNotEmpty(cdLicenseUsage.getActiveServices().getReferences())) {
        services = cdLicenseUsage.getActiveServices()
                       .getReferences()
                       .stream()
                       .map(ReferenceDTO::getName)
                       .collect(Collectors.toSet());
      }
    }
    return services;
  }

  public Set<String> getServicesBeingCreated(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId, String yaml) {
    List<EntitySetupUsageDTO> allReferredUsages =
        getResponseWithRetry(entitySetupUsageClient.listAllReferredUsages(PAGE, SIZE, accountIdentifier,
                                 FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
                                     accountIdentifier, orgIdentifier, projectIdentifier, pipelineId),
                                 EntityType.SERVICE, null),
            "Could not extract setup usage of pipeline with id " + pipelineId + " after {} attempts.");
    List<EntityDetail> entityDetails = PipelineSetupUsageUtils.extractInputReferredEntityFromYaml(
        accountIdentifier, orgIdentifier, projectIdentifier, yaml, allReferredUsages);
    return collectionToStream(entityDetails).map(EntityDetail::getName).collect(Collectors.toSet());
  }

  public int getIncrementForEnforcement(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId, String yaml) {
    Set<String> activeServices = getActiveServices(accountIdentifier);
    Set<String> newServices =
        getServicesBeingCreated(accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, yaml);
    Set<String> difference = Sets.difference(newServices, activeServices);
    return difference.size();
  }

  public void validate(String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId,
      String yaml, String executionId) {
    Integer newServiceCount = newServiceCache.getIfPresent(executionId);
    if (null == newServiceCount) {
      newServiceCount =
          getIncrementForEnforcement(accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, yaml);
      newServiceCache.put(executionId, newServiceCount);
    }

    // update this method
    enforcementClientService.checkAvailability(FeatureRestrictionName.SERVICES, accountIdentifier);
  }
}
