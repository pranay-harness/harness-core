package io.harness.cdng.licenserestriction;

import static io.harness.data.structure.CollectionUtils.collectionToStream;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.licensing.beans.modules.types.CDLicenseType.SERVICES;
import static io.harness.remote.client.NGRestUtils.getResponseWithRetry;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.PipelineSetupUsageUtils;
import io.harness.cdng.usage.beans.CDLicenseUsageDTO;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.licensing.usage.beans.ReferenceDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.licensing.usage.params.CDUsageRequestParams;
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
import org.apache.commons.lang3.tuple.Pair;

@Singleton
public class EnforcementValidator {
  @Inject private LicenseUsageInterface<CDLicenseUsageDTO, CDUsageRequestParams> licenseUsageInterface;
  @Inject private EntitySetupUsageClient entitySetupUsageClient;
  @Inject private EnforcementClientService enforcementClientService;

  private static final int PAGE = 0;
  private static final int SIZE = 100;

  private Cache<String, Integer> newServiceCache =
      CacheBuilder.newBuilder().maximumSize(2000).expireAfterWrite(1, TimeUnit.MINUTES).build();

  public Pair<Set<String>, Integer> getActiveServiceLicenseInfo(String accountIdentifier) {
    CDLicenseUsageDTO licenseUsage = licenseUsageInterface.getLicenseUsage(accountIdentifier, ModuleType.CD,
        new Date().getTime(), CDUsageRequestParams.builder().cdLicenseType(SERVICES).build());
    Set<String> services = new HashSet<>();
    Integer serviceLicenses = 0;
    if (licenseUsage != null) {
      if (null != licenseUsage.getActiveServices() && isNotEmpty(licenseUsage.getActiveServices().getReferences())) {
        services = licenseUsage.getActiveServices()
                       .getReferences()
                       .stream()
                       .map(ReferenceDTO::getName)
                       .collect(Collectors.toSet());

        // TODO: add logic for serviceLicenses
      }
    }
    return Pair.of(services, serviceLicenses);
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

  public int getAdditionalServiceLicenseCount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId, String yaml) {
    Pair<Set<String>, Integer> activeServiceLicenseInfo = getActiveServiceLicenseInfo(accountIdentifier);
    Set<String> newServices =
        getServicesBeingCreated(accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, yaml);
    Set<String> difference = Sets.difference(newServices, activeServiceLicenseInfo.getKey());
    return difference.size();
  }

  public void validate(String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineId,
      String yaml, String executionId) {
    Integer newServiceCount = newServiceCache.getIfPresent(executionId);
    if (null == newServiceCount) {
      newServiceCount =
          getAdditionalServiceLicenseCount(accountIdentifier, orgIdentifier, projectIdentifier, pipelineId, yaml);
      newServiceCache.put(executionId, newServiceCount);
    }

    enforcementClientService.checkAvailabilityWithIncrement(
        FeatureRestrictionName.SERVICES, accountIdentifier, newServiceCount);
  }
}
