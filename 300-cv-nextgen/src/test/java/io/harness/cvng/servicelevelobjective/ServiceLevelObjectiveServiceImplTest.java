package io.harness.cvng.servicelevelobjective;

import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.SLIType;
import io.harness.cvng.servicelevelobjective.beans.SLOTarget;
import io.harness.cvng.servicelevelobjective.beans.SLOTargetType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.beans.slotargetspec.RollingSLOTargetSpec;
import io.harness.cvng.servicelevelobjective.services.ServiceLevelObjectiveService;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceLevelObjectiveServiceImplTest extends CvNextGenTestBase {
  @Inject ServiceLevelObjectiveService serviceLevelObjectiveService;

  @Inject MonitoredServiceService monitoredServiceService;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  List<ServiceLevelIndicator> serviceLevelIndicators;
  SLOTarget sloTarget;
  String userJourneyIdentifier;
  String description;
  String monitoredServiceIdentifier;
  String healthSourceIdentifiers;
  ProjectParams projectParams;
  Map<String, String> tags;
  private BuilderFactory builderFactory;

  @Before
  public void setup() throws IllegalAccessException {
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    identifier = "sloIdentifier";
    name = "sloName";
    monitoredServiceIdentifier = "monitoredServiceIdentifier";
    healthSourceIdentifiers = "healthSourceIdentifier";
    description = "description";
    tags = new HashMap<>();
    tags.put("tag1", "value1");
    tags.put("tag2", "value2");

    serviceLevelIndicators =
        Collections.singletonList(ServiceLevelIndicator.builder()
                                      .identifier("sliIndicator")
                                      .name("sliName")
                                      .type(SLIType.LATENCY)
                                      .spec(ServiceLevelIndicator.SLISpec.builder()
                                                .type(SLIMetricType.THRESHOLD)
                                                .spec(ServiceLevelIndicator.SLISpec.SLIMetricSpec.builder()
                                                          .eventType("eventName")
                                                          .metric1("metric1")
                                                          .metric2("metric2")
                                                          .build())
                                                .build())
                                      .build());
    sloTarget = SLOTarget.builder()
                    .type(SLOTargetType.ROLLING)
                    .sloTargetPercentage(80.0)
                    .spec(RollingSLOTargetSpec.builder().periodLength("30D").build())
                    .build();
    userJourneyIdentifier = "userJourney";

    projectParams = ProjectParams.builder()
                        .accountIdentifier(accountId)
                        .orgIdentifier(orgIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .build();
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreate_Success() {
    ServiceLevelObjectiveDTO sloDTO = createSLOBuilder();
    createMonitoredService();
    ServiceLevelObjectiveResponse serviceLevelObjectiveResponse =
        serviceLevelObjectiveService.create(projectParams, sloDTO);
    assertThat(serviceLevelObjectiveResponse.getServiceLevelObjectiveDTO()).isEqualTo(sloDTO);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreate_withoutMonitoredServiceFailedValidation() {
    ServiceLevelObjectiveDTO sloDTO = createSLOBuilder();
    assertThatThrownBy(() -> serviceLevelObjectiveService.create(projectParams, sloDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format(
            "Monitored Source Entity with identifier %s is not present", sloDTO.getMonitoredServiceRef()));
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testDelete_Success() {
    ServiceLevelObjectiveDTO sloDTO = createSLOBuilder();
    createMonitoredService();
    serviceLevelObjectiveService.create(projectParams, sloDTO);
    boolean isDeleted = serviceLevelObjectiveService.delete(projectParams, sloDTO.getIdentifier());
    assertThat(isDeleted).isEqualTo(true);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testDelete_validationFailedForIncorrectSLO() {
    ServiceLevelObjectiveDTO serviceLevelObjectiveDTO = createSLOBuilder();
    createMonitoredService();
    serviceLevelObjectiveService.create(projectParams, serviceLevelObjectiveDTO);
    serviceLevelObjectiveDTO.setIdentifier("incorrectSLOIdentifier");
    assertThatThrownBy(
        () -> serviceLevelObjectiveService.delete(projectParams, serviceLevelObjectiveDTO.getIdentifier()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format(
            "SLO  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
            serviceLevelObjectiveDTO.getIdentifier(), projectParams.getAccountIdentifier(),
            projectParams.getOrgIdentifier(), projectParams.getProjectIdentifier()));
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testUpdate_Success() {
    ServiceLevelObjectiveDTO sloDTO = createSLOBuilder();
    createMonitoredService();
    ServiceLevelObjectiveResponse serviceLevelObjectiveResponse =
        serviceLevelObjectiveService.create(projectParams, sloDTO);
    assertThat(serviceLevelObjectiveResponse.getServiceLevelObjectiveDTO()).isEqualTo(sloDTO);
    sloDTO.setDescription("newDescription");
    ServiceLevelObjectiveResponse updateServiceLevelObjectiveResponse =
        serviceLevelObjectiveService.update(projectParams, sloDTO.getIdentifier(), sloDTO);
    assertThat(updateServiceLevelObjectiveResponse.getServiceLevelObjectiveDTO()).isEqualTo(sloDTO);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testUpdate_FailedWithEntityNotPresent() {
    ServiceLevelObjectiveDTO sloDTO = createSLOBuilder();
    createMonitoredService();
    ServiceLevelObjectiveResponse serviceLevelObjectiveResponse =
        serviceLevelObjectiveService.create(projectParams, sloDTO);
    assertThat(serviceLevelObjectiveResponse.getServiceLevelObjectiveDTO()).isEqualTo(sloDTO);
    sloDTO.setIdentifier("newIdentifier");
    sloDTO.setDescription("newDescription");
    assertThatThrownBy(() -> serviceLevelObjectiveService.update(projectParams, sloDTO.getIdentifier(), sloDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format(
            "SLO  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
            sloDTO.getIdentifier(), projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
            projectParams.getProjectIdentifier()));
  }

  private ServiceLevelObjectiveDTO createSLOBuilder() {
    return builderFactory.getServiceLevelObjectiveDTOBuilder();
  }

  private void createMonitoredService() {
    MonitoredServiceDTO monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    monitoredServiceDTO.setSources(MonitoredServiceDTO.Sources.builder().build());
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
  }
}
