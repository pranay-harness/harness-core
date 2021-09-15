package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.ANJAN;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.SOWMYA;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MonitoredServiceDataSourceType;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.MonitoredServiceDTOBuilder;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceDependencyDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceSpec;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoredService.MonitoredServiceKeys;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.SetupUsageEventService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;
import io.harness.cvng.models.VerificationType;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonitoredServiceServiceImplTest extends CvNextGenTestBase {
  @Inject MetricPackService metricPackService;
  @Inject CVConfigService cvConfigService;
  @Inject ChangeSourceService changeSourceService;
  @Inject MonitoredServiceService monitoredServiceService;
  @Inject HPersistence hPersistence;
  @Inject ServiceDependencyService serviceDependencyService;
  @Mock NextGenService nextGenService;
  @Mock SetupUsageEventService setupUsageEventService;
  @Mock ChangeSourceService changeSourceServiceMock;

  private BuilderFactory builderFactory;
  String healthSourceName;
  String healthSourceIdentifier;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String environmentIdentifier;
  String serviceIdentifier;
  String feature;
  String connectorIdentifier;
  String appTierName;
  String applicationName;
  String monitoredServiceName;
  String monitoredServiceIdentifier;
  String changeSourceIdentifier;
  String description;
  ProjectParams projectParams;
  ServiceEnvironmentParams environmentParams;
  Map<String, String> tags;

  @Before
  public void setup() throws IllegalAccessException {
    builderFactory = BuilderFactory.getDefault();
    healthSourceName = "healthSourceName";
    healthSourceIdentifier = "healthSourceIdentifier";
    accountId = builderFactory.getContext().getAccountId();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    environmentIdentifier = builderFactory.getContext().getEnvIdentifier();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    feature = "Application Monitoring";
    connectorIdentifier = BuilderFactory.CONNECTOR_IDENTIFIER;
    applicationName = "applicationName";
    appTierName = "appTierName";
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
    monitoredServiceName = "monitoredServiceName";
    monitoredServiceIdentifier = "monitoredServiceIdentifier";
    description = "description";
    changeSourceIdentifier = "changeSourceIdentifier";
    tags = new HashMap<String, String>() {
      {
        put("tag1", "value1");
        put("tag2", "");
      }
    };
    projectParams = ProjectParams.builder()
                        .accountIdentifier(accountId)
                        .orgIdentifier(orgIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .build();
    environmentParams = ServiceEnvironmentParams.builder()
                            .accountIdentifier(accountId)
                            .orgIdentifier(orgIdentifier)
                            .projectIdentifier(projectIdentifier)
                            .serviceIdentifier(serviceIdentifier)
                            .environmentIdentifier(environmentIdentifier)
                            .build();

    FieldUtils.writeField(monitoredServiceService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(monitoredServiceService, "setupUsageEventService", setupUsageEventService, true);
    FieldUtils.writeField(changeSourceService, "changeSourceUpdateHandlerMap", new HashMap<>(), true);
    FieldUtils.writeField(monitoredServiceService, "changeSourceService", changeSourceService, true);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_withFailedValidation() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSource.setName("some-health_source-name");
    monitoredServiceDTO.getSources().getHealthSources().add(healthSource);
    assertThatThrownBy(
        () -> monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            String.format("Multiple Health Sources exists with the same identifier %s", healthSourceIdentifier));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceAlreadyPresentWithSameIdentifier() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    assertThatThrownBy(
        () -> monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessage(String.format(
            "Monitored Source Entity  with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
            monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getOrgIdentifier(),
            monitoredServiceDTO.getProjectIdentifier()));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceAlreadyPresentWithServiceAndEnvironmentRef() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    monitoredServiceDTO.setIdentifier("some-other-identifier");
    assertThatThrownBy(
        () -> monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessage(String.format(
            "Monitored Source Entity  with duplicate service ref %s, environmentRef %s having identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
            monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getEnvironmentRef(),
            monitoredServiceDTO.getIdentifier(), monitoredServiceDTO.getOrgIdentifier(),
            monitoredServiceDTO.getProjectIdentifier()));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceHealthSourcesConfigAlreadyPresent() {
    CVConfig cvConfig =
        AppDynamicsCVConfig.builder()
            .identifier(HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier))
            .accountId(accountId)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .connectorIdentifier(connectorIdentifier)
            .serviceIdentifier(serviceIdentifier)
            .envIdentifier(environmentIdentifier)
            .monitoringSourceName(healthSourceName)
            .productName(feature)
            .category(CVMonitoringCategory.ERRORS)
            .applicationName(applicationName)
            .tierName(appTierName)
            .metricPack(MetricPack.builder().build())
            .build();
    cvConfigService.save(cvConfig);
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    assertThatThrownBy(
        () -> monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessage(String.format(
            "Already Existing configs for Monitored Service  with identifier %s and orgIdentifier %s and projectIdentifier %s",
            HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier),
            orgIdentifier, projectIdentifier));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceWithEmptyHealthSources() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceDTO.setSources(MonitoredServiceDTO.Sources.builder().build());
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceNonEmptyHealthSources() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    AppDynamicsCVConfig cvConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    assertCVConfig(cvConfig, CVMonitoringCategory.ERRORS);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceNonEmptyChangeSource() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    Set<ChangeSourceDTO> changeSources = changeSourceService.get(environmentParams,
        savedMonitoredServiceDTO.getSources()
            .getChangeSources()
            .stream()
            .map(changeSource -> changeSource.getIdentifier())
            .collect(toList()));
    assertThat(changeSources.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpdate_ChangeSourceCreation() {
    MonitoredServiceDTO monitoredServiceDTO =
        createMonitoredServiceDTOBuilder()
            .sources(Sources.builder()
                         .changeSources(
                             new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())))
                         .build())
            .build();
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();

    MonitoredServiceDTO toUpdateMonitoredServiceDTO =
        createMonitoredServiceDTOBuilder()
            .sources(Sources.builder()
                         .changeSources(
                             new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build(),
                                 builderFactory.getPagerDutyChangeSourceDTOBuilder().build())))
                         .build())
            .build();
    MonitoredServiceDTO updatedMonitoredServiceDTO =
        monitoredServiceService.update(builderFactory.getContext().getAccountId(), toUpdateMonitoredServiceDTO)
            .getMonitoredServiceDTO();

    assertThat(updatedMonitoredServiceDTO).isEqualTo(toUpdateMonitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, toUpdateMonitoredServiceDTO);
    Set<ChangeSourceDTO> changeSources = changeSourceService.get(environmentParams,
        updatedMonitoredServiceDTO.getSources()
            .getChangeSources()
            .stream()
            .map(changeSource -> changeSource.getIdentifier())
            .collect(toList()));
    assertThat(changeSources.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_monitoredServiceNonEmptyDependencies() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTOWithDependencies();
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    Set<ServiceDependencyDTO> serviceDependencyDTOS = serviceDependencyService.getDependentServicesForMonitoredService(
        builderFactory.getContext().getProjectParams(), monitoredServiceDTO.getIdentifier());
    assertThat(serviceDependencyDTOS).isEqualTo(monitoredServiceDTO.getDependencies());
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGet_IdentifierNotPresent() {
    assertThatThrownBy(
        () -> monitoredServiceService.get(builderFactory.getContext().getProjectParams(), monitoredServiceIdentifier))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            String.format("Monitored Source Entity with identifier %s is not present", monitoredServiceIdentifier));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGet_usingIdentifier() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredServiceDTO getMonitoredServiceDTO =
        monitoredServiceService.get(builderFactory.getContext().getProjectParams(), monitoredServiceIdentifier)
            .getMonitoredServiceDTO();
    assertThat(monitoredServiceDTO).isEqualTo(getMonitoredServiceDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGet_usingServiceEnvironmentNotPresent() {
    assertThat(monitoredServiceService.get(builderFactory.getContext().getServiceEnvironmentParams())).isNull();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGet_usingServiceEnvironment() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredServiceDTO getMonitoredServiceDTO =
        monitoredServiceService.get(builderFactory.getContext().getServiceEnvironmentParams()).getMonitoredServiceDTO();
    assertThat(monitoredServiceDTO).isEqualTo(getMonitoredServiceDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testDelete() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    boolean isDeleted =
        monitoredServiceService.delete(builderFactory.getContext().getProjectParams(), monitoredServiceIdentifier);
    assertThat(isDeleted).isEqualTo(true);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertThat(monitoredService).isEqualTo(null);
    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(changeSourceService.get(environmentParams, Arrays.asList(changeSourceIdentifier))).isEmpty();
    assertThat(cvConfigs.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testList_withEnvironmentFilter() throws IllegalAccessException {
    useChangeSourceServiceMock();
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    environmentIdentifier = "new-environment";
    monitoredServiceIdentifier = "new-monitored-service-identifier";
    healthSourceIdentifier = "new-health-source-identifier";
    monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    when(nextGenService.listService(anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(ServiceResponse.builder()
                                      .service(builderFactory.serviceResponseDTOBuilder()
                                                   .identifier(serviceIdentifier)
                                                   .name("serviceName")
                                                   .build())
                                      .build()));
    when(nextGenService.listEnvironment(anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(EnvironmentResponse.builder()
                                      .environment(builderFactory.environmentResponseDTOBuilder()
                                                       .identifier(environmentIdentifier)
                                                       .name("environmentName")
                                                       .build())
                                      .build()));
    ChangeSummaryDTO changeSummary = ChangeSummaryDTO.builder().build();
    when(changeSourceServiceMock.getChangeSummary(any(), any(), any(), any())).thenReturn(changeSummary);
    PageResponse<MonitoredServiceListItemDTO> monitoredServiceListDTOPageResponse =
        monitoredServiceService.list(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, 0, 10, null);
    assertThat(monitoredServiceListDTOPageResponse.getTotalPages()).isEqualTo(1);
    assertThat(monitoredServiceListDTOPageResponse.getTotalItems()).isEqualTo(1);
    MonitoredServiceListItemDTO monitoredServiceListItemDTO = monitoredServiceListDTOPageResponse.getContent().get(0);
    assertThat(monitoredServiceListItemDTO.getName()).isEqualTo(monitoredServiceName);
    assertThat(monitoredServiceListItemDTO.getIdentifier()).isEqualTo(monitoredServiceIdentifier);
    assertThat(monitoredServiceListItemDTO.getServiceRef()).isEqualTo(serviceIdentifier);
    assertThat(monitoredServiceListItemDTO.getEnvironmentRef()).isEqualTo(environmentIdentifier);
    assertThat(monitoredServiceListItemDTO.getType()).isEqualTo(MonitoredServiceType.APPLICATION);
    assertThat(monitoredServiceListItemDTO.getChangeSummary()).isEqualTo(changeSummary);
    assertThat(monitoredServiceListItemDTO.isHealthMonitoringEnabled()).isTrue();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testList_forAllEnvironment() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    environmentIdentifier = "new-environment";
    monitoredServiceIdentifier = "new-monitored-service-identifier";
    healthSourceIdentifier = "new-health-source-identifier";
    monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);

    when(nextGenService.listService(anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(ServiceResponse.builder()
                                      .service(builderFactory.serviceResponseDTOBuilder()
                                                   .identifier(serviceIdentifier)
                                                   .name("serviceName")
                                                   .build())
                                      .build()));
    when(nextGenService.listEnvironment(anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(EnvironmentResponse.builder()
                                      .environment(builderFactory.environmentResponseDTOBuilder()
                                                       .identifier(environmentIdentifier)
                                                       .name("environmentName")
                                                       .build())
                                      .build()));
    PageResponse<MonitoredServiceListItemDTO> monitoredServiceListDTOPageResponse =
        monitoredServiceService.list(accountId, orgIdentifier, projectIdentifier, null, 0, 10, null);
    assertThat(monitoredServiceListDTOPageResponse.getTotalPages()).isEqualTo(1);
    assertThat(monitoredServiceListDTOPageResponse.getTotalItems()).isEqualTo(2);
    MonitoredServiceListItemDTO monitoredServiceListItemDTO = monitoredServiceListDTOPageResponse.getContent().get(0);
    assertThat(monitoredServiceListItemDTO.getName()).isEqualTo(monitoredServiceName);
    assertThat(monitoredServiceListItemDTO.getIdentifier()).isEqualTo(monitoredServiceIdentifier);
    assertThat(monitoredServiceListItemDTO.getServiceName()).isEqualTo("serviceName");
    assertThat(monitoredServiceListItemDTO.getServiceRef()).isEqualTo(serviceIdentifier);
    assertThat(monitoredServiceListItemDTO.getEnvironmentName()).isEqualTo("environmentName");
    assertThat(monitoredServiceListItemDTO.getEnvironmentRef()).isEqualTo(environmentIdentifier);
    assertThat(monitoredServiceListItemDTO.getType()).isEqualTo(MonitoredServiceType.APPLICATION);
    assertThat(monitoredServiceListItemDTO.isHealthMonitoringEnabled()).isTrue();
    assertThat(monitoredServiceListItemDTO.getTags()).isEqualTo(tags);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testDeleteByAccountId() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    monitoredServiceService.deleteByAccountIdentifier(MonitoredService.class, accountId);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertThat(monitoredService).isEqualTo(null);
    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testDeleteByProjectIdentifier() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    monitoredServiceService.deleteByProjectIdentifier(
        MonitoredService.class, accountId, orgIdentifier, projectIdentifier);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertThat(monitoredService).isEqualTo(null);
    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testDeleteByOrgIdentifier() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    monitoredServiceService.deleteByOrgIdentifier(MonitoredService.class, accountId, orgIdentifier);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertThat(monitoredService).isEqualTo(null);
    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreateDefault() {
    MonitoredServiceDTO monitoredServiceDTO =
        monitoredServiceService
            .createDefault(builderFactory.getContext().getProjectParams(), serviceIdentifier, environmentIdentifier)
            .getMonitoredServiceDTO();
    assertThat(monitoredServiceDTO.getName()).isEqualTo(serviceIdentifier + "_" + environmentIdentifier);
    assertThat(monitoredServiceDTO.getIdentifier()).isEqualTo(serviceIdentifier + "_" + environmentIdentifier);
    assertThat(monitoredServiceDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(monitoredServiceDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(monitoredServiceDTO.getServiceRef()).isEqualTo(serviceIdentifier);
    assertThat(monitoredServiceDTO.getEnvironmentRef()).isEqualTo(environmentIdentifier);
    assertThat(monitoredServiceDTO.getSources().getHealthSources().size()).isEqualTo(0);
    assertThat(monitoredServiceDTO.getDescription()).isEqualTo("Default Monitored Service");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_monitoredServiceDoesNotExists() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    assertThatThrownBy(
        () -> monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format(
            "Monitored Source Entity  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
            monitoredServiceIdentifier, accountId, orgIdentifier, projectIdentifier));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_serviceRefUpdateNotAllowed() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    monitoredServiceDTO.setServiceRef("new-service-ref");
    assertThatThrownBy(
        () -> monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("serviceRef update is not allowed");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_environmentRefUpdateNotAllowed() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    monitoredServiceDTO.setEnvironmentRef("new-environement-ref");
    assertThatThrownBy(
        () -> monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("environmentRef update is not allowed");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_monitoredServiceBasics() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    monitoredServiceDTO.setName("new-name");
    monitoredServiceDTO.setDescription("new-description");
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceIdentifier);
    assertThat(monitoredService.getName()).isEqualTo("new-name");
    assertThat(monitoredService.getDesc()).isEqualTo("new-description");
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_deletingHealthSource() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());

    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    monitoredServiceDTO.getSources().setHealthSources(null);
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_addingHealthSource() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    AppDynamicsCVConfig alreadySavedCVConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    assertCVConfig(alreadySavedCVConfig, CVMonitoringCategory.ERRORS);

    HealthSource healthSource = createHealthSource(CVMonitoringCategory.PERFORMANCE);
    healthSource.setIdentifier("new-healthSource-identifier");
    monitoredServiceDTO.getSources().getHealthSources().add(healthSource);
    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    assertThat(monitoredService.getHealthSourceIdentifiers().size()).isEqualTo(2);

    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    assertCVConfig((AppDynamicsCVConfig) cvConfigs.get(0), CVMonitoringCategory.ERRORS);

    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, "new-healthSource-identifier"));
    assertThat(cvConfigs.size()).isEqualTo(1);
    assertCVConfig((AppDynamicsCVConfig) cvConfigs.get(0), CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_updatingHealthSource() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    MonitoredService monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    AppDynamicsCVConfig alreadySavedCVConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    assertCVConfig(alreadySavedCVConfig, CVMonitoringCategory.ERRORS);

    HealthSourceSpec healthSourceSpec = createHealthSourceSpec(CVMonitoringCategory.PERFORMANCE);
    monitoredServiceDTO.getSources().getHealthSources().iterator().next().setSpec(healthSourceSpec);

    MonitoredServiceDTO savedMonitoredServiceDTO =
        monitoredServiceService.update(builderFactory.getContext().getAccountId(), monitoredServiceDTO)
            .getMonitoredServiceDTO();
    assertThat(savedMonitoredServiceDTO).isEqualTo(monitoredServiceDTO);
    monitoredService = getMonitoredService(monitoredServiceDTO.getIdentifier());
    assertCommonMonitoredService(monitoredService, monitoredServiceDTO);
    assertThat(monitoredService.getHealthSourceIdentifiers().size()).isEqualTo(1);

    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(cvConfigs.size()).isEqualTo(1);
    assertCVConfig((AppDynamicsCVConfig) cvConfigs.get(0), CVMonitoringCategory.PERFORMANCE);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSetHealthMonitoringFlag() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    monitoredServiceService.setHealthMonitoringFlag(
        builderFactory.getContext().getProjectParams(), monitoredServiceIdentifier, false);
    MonitoredService updatedMonitoredService = getMonitoredService(monitoredServiceIdentifier);
    getCVConfigs(updatedMonitoredService).forEach(cvConfig -> assertThat(cvConfig.isEnabled()).isFalse());
    assertThat(updatedMonitoredService.isEnabled()).isFalse();
    monitoredServiceService.setHealthMonitoringFlag(
        builderFactory.getContext().getProjectParams(), monitoredServiceIdentifier, true);
    updatedMonitoredService = getMonitoredService(monitoredServiceIdentifier);
    assertThat(updatedMonitoredService.isEnabled()).isTrue();
    getCVConfigs(updatedMonitoredService).forEach(cvConfig -> assertThat(cvConfig.isEnabled()).isTrue());
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testListEnvironments() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);

    when(nextGenService.listEnvironment(anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(EnvironmentResponse.builder()
                                      .environment(builderFactory.environmentResponseDTOBuilder()
                                                       .identifier(environmentIdentifier)
                                                       .name("environmentName")
                                                       .build())
                                      .build()));

    List<EnvironmentResponse> environmentResponses =
        monitoredServiceService.listEnvironments(accountId, orgIdentifier, projectIdentifier);
    assertThat(environmentResponses.size()).isEqualTo(1);
    assertThat(environmentResponses.get(0).getEnvironment().getName()).isEqualTo("environmentName");
    assertThat(environmentResponses.get(0).getEnvironment().getIdentifier()).isEqualTo(environmentIdentifier);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testListMonitoredServices() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);

    List<MonitoredService> monitoredServices =
        monitoredServiceService.list(builderFactory.getContext().getProjectParams(),
            monitoredServiceDTO.getServiceRef(), monitoredServiceDTO.getEnvironmentRef());
    assertThat(monitoredServices.size()).isEqualTo(1);

    monitoredServices = monitoredServiceService.list(
        builderFactory.getContext().getProjectParams(), monitoredServiceDTO.getServiceRef(), null);
    assertThat(monitoredServices.size()).isEqualTo(1);

    monitoredServices = monitoredServiceService.list(
        builderFactory.getContext().getProjectParams(), null, monitoredServiceDTO.getEnvironmentRef());
    assertThat(monitoredServices.size()).isEqualTo(1);

    monitoredServices = monitoredServiceService.list(builderFactory.getContext().getProjectParams(), null, null);
    assertThat(monitoredServices.size()).isEqualTo(1);

    assertThatThrownBy(() -> monitoredServiceService.list(null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("projectParams is marked @NonNull but is null");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeEvents() throws IllegalAccessException {
    useChangeSourceServiceMock();
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    List<ChangeEventDTO> changeEventDTOS = Arrays.asList(builderFactory.getHarnessCDChangeEventDTOBuilder().build());
    when(changeSourceServiceMock.getChangeEvents(eq(builderFactory.getContext().getServiceEnvironmentParams()),
             eq(hPersistence.createQuery(MonitoredService.class).get().getChangeSourceIdentifiers()),
             eq(Instant.ofEpochSecond(100)), eq(Instant.ofEpochSecond(100)), eq(new ArrayList<>())))
        .thenReturn(changeEventDTOS);
    List<ChangeEventDTO> result =
        monitoredServiceService.getChangeEvents(builderFactory.getContext().getProjectParams(),
            monitoredServiceIdentifier, Instant.ofEpochSecond(100), Instant.ofEpochSecond(100), new ArrayList<>());
    assertThat(result).isEqualTo(changeEventDTOS);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testgetChangeSummary() throws IllegalAccessException {
    useChangeSourceServiceMock();
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    ChangeSummaryDTO changeSummaryDTO = ChangeSummaryDTO.builder().build();
    when(changeSourceServiceMock.getChangeSummary(eq(builderFactory.getContext().getServiceEnvironmentParams()),
             eq(hPersistence.createQuery(MonitoredService.class).get().getChangeSourceIdentifiers()),
             eq(Instant.ofEpochSecond(100)), eq(Instant.ofEpochSecond(100))))
        .thenReturn(changeSummaryDTO);
    ChangeSummaryDTO result = monitoredServiceService.getChangeSummary(builderFactory.getContext().getProjectParams(),
        monitoredServiceIdentifier, Instant.ofEpochSecond(100), Instant.ofEpochSecond(100));
    assertThat(result).isEqualTo(changeSummaryDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetHealthSources() {
    MonitoredServiceDTO monitoredServiceDTO = createMonitoredServiceDTO();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
    List<HealthSourceDTO> healthSourceDTOS = monitoredServiceService.getHealthSources(environmentParams);
    assertThat(healthSourceDTOS.size()).isEqualTo(1);
    assertThat(healthSourceDTOS.get(0).getIdentifier())
        .isEqualTo(HealthSourceService.getNameSpacedIdentifier(monitoredServiceIdentifier, healthSourceIdentifier));
    assertThat(healthSourceDTOS.get(0).getType()).isEqualTo(DataSourceType.APP_DYNAMICS);
    assertThat(healthSourceDTOS.get(0).getVerificationType()).isEqualTo(VerificationType.TIME_SERIES);
    assertThat(healthSourceDTOS.get(0).getName()).isEqualTo(healthSourceName);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testGetYamlTemplate() {
    assert (monitoredServiceService.getYamlTemplate(projectParams, MonitoredServiceType.APPLICATION))
        .equals("monitoredService:\n"
            + "  identifier:\n"
            + "  type: Application\n"
            + "  name:\n"
            + "  desc:\n"
            + "  projectIdentifier: " + projectParams.getProjectIdentifier() + "\n"
            + "  orgIdentifier: " + projectParams.getOrgIdentifier() + "\n"
            + "  serviceRef:\n"
            + "  environmentRef:\n"
            + "  sources:\n"
            + "    healthSources:\n"
            + "    changeSources:\n"
            + "      - name: Harness CD\n"
            + "        identifier: harness_cd\n"
            + "        type: HarnessCD\n"
            + "        enabled : true\n");

    assert (monitoredServiceService.getYamlTemplate(projectParams, MonitoredServiceType.INFRASTRUCTURE))
        .equals("monitoredService:\n"
            + "  identifier:\n"
            + "  type: Infrastructure\n"
            + "  name:\n"
            + "  desc:\n"
            + "  projectIdentifier: " + projectParams.getProjectIdentifier() + "\n"
            + "  orgIdentifier: " + projectParams.getOrgIdentifier() + "\n"
            + "  serviceRef:\n"
            + "  environmentRef:\n"
            + "  sources:\n"
            + "    healthSources:\n"
            + "    changeSources:\n");

    assert (monitoredServiceService.getYamlTemplate(projectParams, null))
        .equals("monitoredService:\n"
            + "  identifier:\n"
            + "  type: Application\n"
            + "  name:\n"
            + "  desc:\n"
            + "  projectIdentifier: " + projectParams.getProjectIdentifier() + "\n"
            + "  orgIdentifier: " + projectParams.getOrgIdentifier() + "\n"
            + "  serviceRef:\n"
            + "  environmentRef:\n"
            + "  sources:\n"
            + "    healthSources:\n"
            + "    changeSources:\n"
            + "      - name: Harness CD\n"
            + "        identifier: harness_cd\n"
            + "        type: HarnessCD\n"
            + "        enabled : true\n");
  }

  MonitoredServiceDTO createMonitoredServiceDTO() {
    return createMonitoredServiceDTOBuilder()
        .sources(
            MonitoredServiceDTO.Sources.builder()
                .healthSources(
                    Arrays.asList(createHealthSource(CVMonitoringCategory.ERRORS)).stream().collect(Collectors.toSet()))
                .changeSources(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())
                                   .stream()
                                   .collect(Collectors.toSet()))
                .build())
        .build();
  }

  MonitoredServiceDTO createMonitoredServiceDTOWithDependencies() {
    return createMonitoredServiceDTOBuilder()
        .sources(
            MonitoredServiceDTO.Sources.builder()
                .healthSources(
                    Arrays.asList(createHealthSource(CVMonitoringCategory.ERRORS)).stream().collect(Collectors.toSet()))
                .build())
        .dependencies(
            Sets.newHashSet(ServiceDependencyDTO.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build(),
                ServiceDependencyDTO.builder().monitoredServiceIdentifier(randomAlphanumeric(20)).build()))
        .build();
  }

  private MonitoredServiceDTOBuilder createMonitoredServiceDTOBuilder() {
    return builderFactory.monitoredServiceDTOBuilder()
        .identifier(monitoredServiceIdentifier)
        .serviceRef(serviceIdentifier)
        .environmentRef(environmentIdentifier)
        .name(monitoredServiceName)
        .tags(tags);
  }

  HealthSource createHealthSource(CVMonitoringCategory cvMonitoringCategory) {
    return HealthSource.builder()
        .identifier(healthSourceIdentifier)
        .name(healthSourceName)
        .type(MonitoredServiceDataSourceType.APP_DYNAMICS)
        .spec(createHealthSourceSpec(cvMonitoringCategory))
        .build();
  }

  HealthSourceSpec createHealthSourceSpec(CVMonitoringCategory cvMonitoringCategory) {
    return AppDynamicsHealthSourceSpec.builder()
        .applicationName(applicationName)
        .tierName(appTierName)
        .connectorRef(connectorIdentifier)
        .feature(feature)
        .metricPacks(new HashSet<MetricPackDTO>() {
          { add(MetricPackDTO.builder().identifier(cvMonitoringCategory).build()); }
        })
        .build();
  }

  void assertCommonMonitoredService(MonitoredService monitoredService, MonitoredServiceDTO monitoredServiceDTO) {
    assertThat(monitoredService.getName()).isEqualTo(monitoredServiceDTO.getName());
    assertThat(monitoredService.getIdentifier()).isEqualTo(monitoredServiceDTO.getIdentifier());
    assertThat(monitoredService.getAccountId()).isEqualTo(accountId);
    assertThat(monitoredService.getDesc()).isEqualTo(monitoredServiceDTO.getDescription());
    assertThat(monitoredService.getServiceIdentifier()).isEqualTo(monitoredServiceDTO.getServiceRef());
    assertThat(monitoredService.getEnvironmentIdentifier()).isEqualTo(monitoredServiceDTO.getEnvironmentRef());
    assertThat(monitoredService.getOrgIdentifier()).isEqualTo(monitoredServiceDTO.getOrgIdentifier());
    assertThat(monitoredService.getProjectIdentifier()).isEqualTo(monitoredServiceDTO.getProjectIdentifier());
    assertThat(monitoredService.getTags()).isEqualTo(TagMapper.convertToList(monitoredServiceDTO.getTags()));
    assertThat(monitoredService.getHealthSourceIdentifiers())
        .isEqualTo(monitoredServiceDTO.getSources()
                       .getHealthSources()
                       .stream()
                       .map(source -> source.getIdentifier())
                       .collect(toList()));
    assertThat(monitoredService.getProjectIdentifier()).isEqualTo(monitoredServiceDTO.getProjectIdentifier());
  }
  private void assertCVConfig(AppDynamicsCVConfig cvConfig, CVMonitoringCategory cvMonitoringCategory) {
    assertThat(cvConfig.getAccountId()).isEqualTo(accountId);
    assertThat(cvConfig.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(cvConfig.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(cvConfig.getEnvIdentifier()).isEqualTo(environmentIdentifier);
    assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(cvConfig.getProductName()).isEqualTo(feature);
    assertThat(cvConfig.getMonitoringSourceName()).isEqualTo(healthSourceName);
    assertThat(cvConfig.getConnectorIdentifier()).isEqualTo(connectorIdentifier);
    assertThat(cvConfig.getTierName()).isEqualTo(appTierName);
    assertThat(cvConfig.getApplicationName()).isEqualTo(applicationName);
    assertThat(cvConfig.getApplicationName()).isEqualTo(applicationName);
    assertThat(cvConfig.getCategory()).isEqualTo(cvMonitoringCategory);
    metricPackService.getMetricPack(
        accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS, cvMonitoringCategory);
  }

  private MonitoredService getMonitoredService(String identifier) {
    return hPersistence.createQuery(MonitoredService.class)
        .filter(MonitoredServiceKeys.accountId, accountId)
        .filter(MonitoredServiceKeys.orgIdentifier, orgIdentifier)
        .filter(MonitoredServiceKeys.projectIdentifier, projectIdentifier)
        .filter(MonitoredServiceKeys.identifier, identifier)
        .get();
  }

  private List<CVConfig> getCVConfigs(MonitoredService monitoredService) {
    return hPersistence.createQuery(CVConfig.class)
        .filter(CVConfigKeys.accountId, accountId)
        .filter(CVConfigKeys.orgIdentifier, orgIdentifier)
        .filter(CVConfigKeys.projectIdentifier, projectIdentifier)
        .field(CVConfigKeys.identifier)
        .in(monitoredService.getHealthSourceIdentifiers()
                .stream()
                .map(identifier
                    -> HealthSourceService.getNameSpacedIdentifier(monitoredService.getIdentifier(), identifier))
                .collect(toList()))
        .asList();
  }

  private void useChangeSourceServiceMock() throws IllegalAccessException {
    FieldUtils.writeField(monitoredServiceService, "changeSourceService", changeSourceServiceMock, true);
  }
}
