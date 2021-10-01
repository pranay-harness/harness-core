package io.harness.cdng.usage.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.usage.beans.CDLicenseUsageConstants;
import io.harness.cdng.usage.beans.CDLicenseUsageDTO;
import io.harness.dtos.InstanceDTO;
import io.harness.licensing.usage.beans.ReferenceDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.service.instance.InstanceService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class CDLicenseUsageImpl implements LicenseUsageInterface<CDLicenseUsageDTO> {
  @Inject InstanceService instanceService;
  @Inject ServiceEntityService serviceEntityService;

  @Override
  public CDLicenseUsageDTO getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp) {
    Preconditions.checkArgument(timestamp > 0, format("Invalid timestamp %d while fetching LicenseUsages.", timestamp));
    Preconditions.checkArgument(ModuleType.CD == module, format("Invalid Module type %s provided", module.toString()));

    List<InstanceDTO> activeInstancesByAccount = instanceService.getInstancesDeployedAfter(
        accountIdentifier, getEpochMilliNDaysAgo(timestamp, CDLicenseUsageConstants.TIME_PERIOD_IN_DAYS));

    UsageDataDTO activeServiceInstances = getActiveServiceInstancesUsageDTO(activeInstancesByAccount);
    UsageDataDTO activeServices = getActiveServicesUsageDTO(activeInstancesByAccount);

    return CDLicenseUsageDTO.builder()
        .activeServices(activeServices)
        .activeServiceInstances(activeServiceInstances)
        .accountIdentifier(accountIdentifier)
        .timestamp(timestamp)
        .module(module.getDisplayName())
        .build();
  }

  private UsageDataDTO getActiveServicesUsageDTO(List<InstanceDTO> activeInstancesByAccount) {
    if (isEmpty(activeInstancesByAccount)) {
      return UsageDataDTO.builder()
          .count(0)
          .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
          .references(emptyList())
          .build();
    }

    Map<String, ReferenceDTO> referenceDTOMap = new HashMap<>();
    activeInstancesByAccount.stream()
        .map(this::createActiveServiceReferenceDTOFromInstanceDTO)
        .forEach(serviceReferenceDTO -> referenceDTOMap.put(serviceReferenceDTO.getIdentifier(), serviceReferenceDTO));

    return UsageDataDTO.builder()
        .count(referenceDTOMap.size())
        .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
        .references(new ArrayList<>(referenceDTOMap.values()))
        .build();
  }

  private ReferenceDTO createActiveServiceReferenceDTOFromInstanceDTO(InstanceDTO instanceDTO) {
    ServiceEntity serviceEntity = serviceEntityService.find(instanceDTO.getAccountIdentifier(),
        instanceDTO.getOrgIdentifier(), instanceDTO.getProjectIdentifier(), instanceDTO.getServiceIdentifier(), false);
    return createReferenceDTOFromServiceEntity(serviceEntity);
  }

  private UsageDataDTO getActiveServiceInstancesUsageDTO(List<InstanceDTO> activeInstancesByAccount) {
    if (isEmpty(activeInstancesByAccount)) {
      return UsageDataDTO.builder()
          .count(0)
          .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
          .references(emptyList())
          .build();
    }

    List<ReferenceDTO> references = new ArrayList<>();
    activeInstancesByAccount.stream().map(this::createReferenceDTOForInstance).forEach(references::add);

    return UsageDataDTO.builder()
        .count(activeInstancesByAccount.size())
        .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
        .references(references)
        .build();
  }

  private ReferenceDTO createReferenceDTOForInstance(InstanceDTO instanceDTO) {
    if (null == instanceDTO) {
      return ReferenceDTO.builder().build();
    }
    return ReferenceDTO.builder()
        .identifier(instanceDTO.getInstanceKey())
        .name(instanceDTO.getInstanceKey())
        .accountIdentifier(instanceDTO.getAccountIdentifier())
        .orgIdentifier(instanceDTO.getOrgIdentifier())
        .projectIdentifier(instanceDTO.getProjectIdentifier())
        .build();
  }

  private ReferenceDTO createReferenceDTOFromServiceEntity(ServiceEntity serviceEntity) {
    if (null == serviceEntity) {
      return ReferenceDTO.builder().build();
    }
    return ReferenceDTO.builder()
        .identifier(serviceEntity.getIdentifier())
        .name(serviceEntity.getName())
        .accountIdentifier(serviceEntity.getAccountId())
        .orgIdentifier(serviceEntity.getOrgIdentifier())
        .projectIdentifier(serviceEntity.getProjectIdentifier())
        .build();
  }

  private long getEpochMilliNDaysAgo(long timestamp, int days) {
    return Instant.ofEpochSecond(timestamp).minus(Period.ofDays(days)).toEpochMilli();
  }
}
