/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.usage.impl;

import static io.harness.cdng.usage.beans.CDLicenseUsageConstants.LICENSE_INSTANCE_LIMIT;
import static io.harness.cdng.usage.beans.CDLicenseUsageConstants.PERCENTILE;
import static io.harness.cdng.usage.beans.CDLicenseUsageConstants.TIME_PERIOD_IN_DAYS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.licensing.beans.modules.types.CDLicenseType.SERVICES;
import static io.harness.licensing.beans.modules.types.CDLicenseType.SERVICE_INSTANCES;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.usage.beans.CDLicenseUsageConstants;
import io.harness.cdng.usage.beans.CDLicenseUsageDTO;
import io.harness.cdng.usage.beans.ServiceInstanceUsageDTO;
import io.harness.cdng.usage.beans.ServiceUsageDTO;
import io.harness.dtos.InstanceDTO;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.licensing.usage.beans.ReferenceDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.licensing.usage.params.CDUsageRequestParams;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.service.instance.InstanceService;
import io.harness.timescaledb.tables.pojos.Services;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record3;
import org.jooq.Table;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class CDLicenseUsageImpl implements LicenseUsageInterface<CDLicenseUsageDTO, CDUsageRequestParams> {
  @Inject CDLicenseUsageDslHelper cdLicenseUsageHelper;
  @Inject InstanceService instanceService;
  @Inject ServiceEntityService serviceEntityService;

  @Override
  public CDLicenseUsageDTO getLicenseUsage(
      String accountIdentifier, ModuleType module, long timestamp, CDUsageRequestParams usageRequest) {
    Preconditions.checkArgument(timestamp > 0, format("Invalid timestamp %d while fetching LicenseUsages.", timestamp));
    Preconditions.checkArgument(ModuleType.CD == module, format("Invalid Module type %s provided", module.toString()));

    long startInterval = getEpochMilliNDaysAgo(timestamp, TIME_PERIOD_IN_DAYS);

    List<InstanceDTO> activeInstancesByAccount =
        instanceService.getInstancesDeployedInInterval(accountIdentifier, startInterval, timestamp);
    Table<Record3<String, String, String>> serviceTableFromInstances =
        cdLicenseUsageHelper.getOrgProjectServiceTableFromInstances(activeInstancesByAccount);

    List<AggregateServiceUsageInfo> activeServicesUsageInfo = new ArrayList<>();
    if (serviceTableFromInstances != null) {
      activeServicesUsageInfo = cdLicenseUsageHelper.getActiveServicesInfoWithPercentileServiceInstanceCount(
          accountIdentifier, PERCENTILE, startInterval, timestamp, serviceTableFromInstances);
    }

    UsageDataDTO activeServices =
        getActiveServicesUsageDTO(activeServicesUsageInfo, accountIdentifier, serviceTableFromInstances);
    UsageDataDTO serviceLicenseUsed = getServiceLicenseUsedDTO(usageRequest, activeServicesUsageInfo);
    UsageDataDTO activeServiceInstances =
        getActiveServiceInstancesDTO(accountIdentifier, activeServicesUsageInfo, activeInstancesByAccount);

    switch (usageRequest.getCdLicenseType()) {
      case SERVICES:
        return ServiceUsageDTO.builder()
            .activeServices(activeServices)
            .activeServiceInstances(activeServiceInstances)
            .serviceLicenses(serviceLicenseUsed)
            .cdLicenseType(SERVICES)
            .accountIdentifier(accountIdentifier)
            .module(module.getDisplayName())
            .build();
      case SERVICE_INSTANCES:
        return ServiceInstanceUsageDTO.builder()
            .activeServices(activeServices)
            .activeServiceInstances(activeServiceInstances)
            .cdLicenseType(SERVICE_INSTANCES)
            .accountIdentifier(accountIdentifier)
            .module(module.getDisplayName())
            .build();
      default:
        throw new InvalidArgumentsException("Invalid License Type.", WingsException.USER);
    }
  }

  @Nullable
  private UsageDataDTO getServiceLicenseUsedDTO(
      CDUsageRequestParams usageRequest, List<AggregateServiceUsageInfo> activeServicesInfo) {
    UsageDataDTO serviceLicenseUsed = null;
    if (usageRequest.getCdLicenseType().equals(SERVICES) && isNotEmpty(activeServicesInfo)) {
      long cumulativeLicenseCount = getCumulativeLicenseCount(activeServicesInfo);
      serviceLicenseUsed = getServiceLicenseUseDTO(activeServicesInfo, cumulativeLicenseCount);
    }
    return serviceLicenseUsed;
  }

  private UsageDataDTO getActiveServiceInstancesDTO(String accountIdentifier,
      List<AggregateServiceUsageInfo> activeServicesUsageInfo, List<InstanceDTO> activeInstancesByAccount) {
    long aggregatedPercentileInstanceCount = getAggregatedServiceInstanceCount(activeServicesUsageInfo);

    return createActiveServiceInstancesUsageDTO(activeInstancesByAccount, aggregatedPercentileInstanceCount);
  }

  private long getAggregatedServiceInstanceCount(List<AggregateServiceUsageInfo> activeServicesUsageInfo) {
    if (isEmpty(activeServicesUsageInfo)) {
      return 0;
    }
    return activeServicesUsageInfo.stream().mapToLong(AggregateServiceUsageInfo::getActiveInstanceCount).sum();
  }

  private UsageDataDTO getServiceLicenseUseDTO(
      List<AggregateServiceUsageInfo> activeServiceUsageInfoList, long cumulativeLicenseCount) {
    if (isEmpty(activeServiceUsageInfoList)) {
      return UsageDataDTO.builder().count(0).displayName(CDLicenseUsageConstants.DISPLAY_NAME).build();
    }

    return UsageDataDTO.builder()
        .count(cumulativeLicenseCount)
        .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
        .build();
  }

  private long getCumulativeLicenseCount(List<AggregateServiceUsageInfo> serviceUsageInfoList) {
    return serviceUsageInfoList.stream()
        .map(serviceUsageInfo -> getLicencesCount(serviceUsageInfo.getActiveInstanceCount()))
        .reduce(0L, Long::sum);
  }
  private static long getLicencesCount(long activeInstanceCount) {
    return (activeInstanceCount + LICENSE_INSTANCE_LIMIT - 1) / LICENSE_INSTANCE_LIMIT;
  }

  private UsageDataDTO getActiveServicesUsageDTO(List<AggregateServiceUsageInfo> activeServiceUsageInfoList,
      String accountIdentifier, Table<Record3<String, String, String>> orgProjectServiceTable) {
    if (isEmpty(activeServiceUsageInfoList)) {
      return UsageDataDTO.builder()
          .count(0)
          .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
          .references(emptyList())
          .build();
    }

    List<Services> services = cdLicenseUsageHelper.getServiceEntities(accountIdentifier, orgProjectServiceTable);
    List<ReferenceDTO> activeServiceReferenceDTOList =
        services.stream()
            .map(service -> createReferenceDTOFromService(accountIdentifier, service))
            .collect(Collectors.toList());

    return UsageDataDTO.builder()
        .count(activeServiceReferenceDTOList.size())
        .displayName(CDLicenseUsageConstants.DISPLAY_NAME)
        .references(activeServiceReferenceDTOList)
        .build();
  }

  private ReferenceDTO createReferenceDTOFromService(String accountIdentifier, Services service) {
    if (null == service) {
      return ReferenceDTO.builder().build();
    }
    return ReferenceDTO.builder()
        .identifier(service.getIdentifier())
        .name(service.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(service.getOrgIdentifier())
        .projectIdentifier(service.getProjectIdentifier())
        .build();
  }

  private UsageDataDTO createActiveServiceInstancesUsageDTO(
      List<InstanceDTO> activeInstancesByAccount, long aggregatedCount) {
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
        .count(aggregatedCount)
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

  private long getEpochMilliNDaysAgo(long timestamp, int days) {
    return Instant.ofEpochMilli(timestamp).minus(Period.ofDays(days)).toEpochMilli();
  }
}
