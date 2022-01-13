/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.usage.impl;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.usage.beans.ServiceInstanceUsageDTO;
import io.harness.cdng.usage.beans.ServiceUsageDTO;
import io.harness.dtos.InstanceDTO;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.licensing.usage.beans.ReferenceDTO;
import io.harness.licensing.usage.params.CDUsageRequestParams;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.service.instance.InstanceService;
import io.harness.timescaledb.tables.pojos.Services;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Table;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class CDLicenseUsageImplTest extends CategoryTest {
  @Mock private InstanceService instanceService;
  @Mock private ServiceEntityService serviceEntityService;
  @Mock CDLicenseUsageDslHelper cdLicenseUsageHelper;
  @InjectMocks @Inject private CDLicenseUsageImpl cdLicenseUsage;

  private static final String accountIdentifier = "ACCOUNT_ID";
  private static final String orgIdentifier = "ORG_ID";
  private static final String projectIdentifier = "PROJECT_ID";
  private static final String instanceKey = "INSTANCE";
  private static final String serviceIdentifier = "SERVICE";
  private static final String envIdentifier = "ENV_ID";
  private static final long timestamp = 1632066423L;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testGetServiceTypeLicenseUsage() {
    prepareTestData();

    ServiceUsageDTO cdServiceUsageDTO = (ServiceUsageDTO) cdLicenseUsage.getLicenseUsage(accountIdentifier,
        ModuleType.CD, timestamp, CDUsageRequestParams.builder().cdLicenseType(CDLicenseType.SERVICES).build());

    verify(instanceService, times(1))
        .getInstancesDeployedInInterval(eq(accountIdentifier),
            eq(Instant.ofEpochMilli(timestamp).minus(Period.ofDays(30)).toEpochMilli()), eq(timestamp));

    assertActiveInstanceUsageDTOOutput(cdServiceUsageDTO);
    assertActiveServiceDTOOutput(cdServiceUsageDTO);

    assertThat(cdServiceUsageDTO.getServiceLicenses()).isNotNull();
    assertThat(cdServiceUsageDTO.getServiceLicenses().getCount()).isEqualTo(6);
    assertThat(cdServiceUsageDTO.getServiceLicenses().getReferences()).isNull();
  }

  private void assertActiveServiceDTOOutput(ServiceUsageDTO cdServiceUsageDTO) {
    assertThat(cdServiceUsageDTO.getActiveServices()).isNotNull();
    assertThat(cdServiceUsageDTO.getActiveServices().getCount()).isEqualTo(3);
    assertThat(cdServiceUsageDTO.getActiveServices().getReferences().size()).isEqualTo(3);
    assertThat(cdServiceUsageDTO.getActiveServices()).isNotNull();
    List<ReferenceDTO> activeServiceReferences = cdServiceUsageDTO.getActiveServices().getReferences();
    ReferenceDTO expectedActiveServiceReference = getExpectedActiveServiceReference();
    assertThat(activeServiceReferences.get(0))
        .isEqualToComparingOnlyGivenFields(expectedActiveServiceReference, "identifier", "name", "accountIdentifier",
            "projectIdentifier", "orgIdentifier");
  }

  private void assertActiveInstanceUsageDTOOutput(ServiceUsageDTO cdServiceUsageDTO) {
    assertThat(cdServiceUsageDTO.getActiveServiceInstances()).isNotNull();
    assertThat(cdServiceUsageDTO.getActiveServiceInstances().getCount()).isEqualTo(90);
    List<ReferenceDTO> activeServiceInstanceReferences = cdServiceUsageDTO.getActiveServiceInstances().getReferences();
    assertThat(activeServiceInstanceReferences).hasSize(3);
    ReferenceDTO expectedInstanceReference = getExpectedInstanceReference();
    assertThat(activeServiceInstanceReferences.get(0))
        .isEqualToComparingOnlyGivenFields(
            expectedInstanceReference, "identifier", "name", "accountIdentifier", "projectIdentifier", "orgIdentifier");
  }

  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testGetServiceInstanceTypeLicenseUsage() {
    prepareTestData();
    ServiceInstanceUsageDTO cdServiceInstanceUsageDTO =
        (ServiceInstanceUsageDTO) cdLicenseUsage.getLicenseUsage(accountIdentifier, ModuleType.CD, timestamp,
            CDUsageRequestParams.builder().cdLicenseType(CDLicenseType.SERVICE_INSTANCES).build());

    verify(instanceService, times(1))
        .getInstancesDeployedInInterval(eq(accountIdentifier),
            eq(Instant.ofEpochMilli(timestamp).minus(Period.ofDays(30)).toEpochMilli()), eq(timestamp));

    assertThat(cdServiceInstanceUsageDTO.getActiveServiceInstances()).isNotNull();
    List<ReferenceDTO> activeServiceInstanceReferences =
        cdServiceInstanceUsageDTO.getActiveServiceInstances().getReferences();
    assertThat(activeServiceInstanceReferences).hasSize(3);
    ReferenceDTO expectedInstanceReference = getExpectedInstanceReference();
    assertThat(activeServiceInstanceReferences.get(0))
        .isEqualToComparingOnlyGivenFields(
            expectedInstanceReference, "identifier", "name", "accountIdentifier", "projectIdentifier", "orgIdentifier");

    assertThat(cdServiceInstanceUsageDTO.getActiveServices()).isNotNull();
    assertThat(cdServiceInstanceUsageDTO.getActiveServices().getCount()).isEqualTo(3);
    assertThat(cdServiceInstanceUsageDTO.getActiveServices().getReferences().size()).isEqualTo(3);
    assertThat(cdServiceInstanceUsageDTO.getActiveServices()).isNotNull();
    List<ReferenceDTO> activeServiceReferences = cdServiceInstanceUsageDTO.getActiveServices().getReferences();
    ReferenceDTO expectedActiveServiceReference = getExpectedActiveServiceReference();
    assertThat(activeServiceReferences.get(0))
        .isEqualToComparingOnlyGivenFields(expectedActiveServiceReference, "identifier", "name", "accountIdentifier",
            "projectIdentifier", "orgIdentifier");
  }

  @Test
  @Owner(developers = OwnerRule.TATHAGAT)
  @Category(UnitTests.class)
  public void testGetLicenseUsageEmptyAggregateServiceUsageInfo() {
    when(cdLicenseUsageHelper.getActiveServicesInfoWithPercentileServiceInstanceCount(
             anyString(), anyDouble(), anyLong(), anyLong(), any(Table.class)))
        .thenReturn(emptyList());

    ServiceUsageDTO serviceTypeLicenseUsage = (ServiceUsageDTO) cdLicenseUsage.getLicenseUsage(accountIdentifier,
        ModuleType.CD, timestamp, CDUsageRequestParams.builder().cdLicenseType(CDLicenseType.SERVICES).build());

    verify(instanceService, times(1))
        .getInstancesDeployedInInterval(eq(accountIdentifier),
            eq(Instant.ofEpochMilli(timestamp).minus(Period.ofDays(30)).toEpochMilli()), eq(timestamp));
    verify(serviceEntityService, times(0)).find(anyString(), anyString(), anyString(), anyString(), anyBoolean());

    assertThat(serviceTypeLicenseUsage.getActiveServices().getCount()).isZero();
    assertThat(serviceTypeLicenseUsage.getActiveServiceInstances().getCount()).isZero();
    assertThat(serviceTypeLicenseUsage.getServiceLicenses()).isNull();
  }

  private void prepareTestData() {
    List<InstanceDTO> testInstanceDTOData = createTestInstanceDTOData(3);
    List<Services> testServiceData = createTestServiceData(3);
    List<AggregateServiceUsageInfo> testServiceUsageInfoData = createTestServiceUsageInfoData(3);

    when(cdLicenseUsageHelper.getActiveServicesInfoWithPercentileServiceInstanceCount(
             anyString(), anyDouble(), anyLong(), anyLong(), any(Table.class)))
        .thenReturn(testServiceUsageInfoData);
    when(instanceService.getInstancesDeployedInInterval(anyString(), anyLong(), anyLong()))
        .thenReturn(testInstanceDTOData);
    when(cdLicenseUsageHelper.getServiceEntities(any(), any())).thenReturn(testServiceData);
    when(cdLicenseUsageHelper.getOrgProjectServiceTableFromInstances(any())).thenCallRealMethod();
    when(cdLicenseUsageHelper.getOrgProjectServiceRows(any())).thenCallRealMethod();
  }

  private ReferenceDTO getExpectedActiveServiceReference() {
    return ReferenceDTO.builder()
        .name("SERVICE0")
        .identifier("SERVICE0")
        .accountIdentifier("ACCOUNT_ID")
        .projectIdentifier("PROJECT_ID0")
        .orgIdentifier("ORG_ID0")
        .build();
  }

  private ReferenceDTO getExpectedInstanceReference() {
    return ReferenceDTO.builder()
        .identifier("INSTANCE0")
        .name("INSTANCE0")
        .accountIdentifier("ACCOUNT_ID0")
        .projectIdentifier("PROJECT_ID0")
        .orgIdentifier("ORG_ID0")
        .build();
  }

  List<InstanceDTO> createTestInstanceDTOData(int dataSize) {
    List<InstanceDTO> instanceDTOList = new ArrayList<>();
    for (int i = 0; i < dataSize; i++) {
      instanceDTOList.add(InstanceDTO.builder()
                              .instanceKey(instanceKey + i)
                              .accountIdentifier(accountIdentifier + i)
                              .projectIdentifier(projectIdentifier + i)
                              .orgIdentifier(orgIdentifier + i)
                              .envIdentifier(envIdentifier + i)
                              .serviceIdentifier(serviceIdentifier + i)
                              .build());
    }
    return instanceDTOList;
  }

  private List<Services> createTestServiceData(int dataSize) {
    List<Services> services = new ArrayList<>();
    for (int i = 0; i < dataSize; i++) {
      services.add(new Services(serviceIdentifier + i, accountIdentifier + i, orgIdentifier + i, projectIdentifier + i,
          serviceIdentifier + i, serviceIdentifier + i, false, null, null));
    }
    return services;
  }

  private List<AggregateServiceUsageInfo> createTestServiceUsageInfoData(int dataSize) {
    List<AggregateServiceUsageInfo> serviceUsageInfoList = new ArrayList<>();
    for (int i = 1; i <= dataSize; i++) {
      serviceUsageInfoList.add(
          new AggregateServiceUsageInfo(orgIdentifier, projectIdentifier, serviceIdentifier + i, 15L * i));
    }
    return serviceUsageInfoList;
  }
}
