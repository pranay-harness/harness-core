package io.harness.cvng.servicelevelobjective;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.sli.SLIOnboardingGraphs;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.AppDynamicsServiceimplTest;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorType;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.RatioSLIMetricEventType;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.RatioSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdType;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceLevelIndicatorServiceImplTest extends CvNextGenTestBase {
  BuilderFactory builderFactory;
  @Inject ServiceLevelIndicatorService serviceLevelIndicatorService;
  @Inject HPersistence hPersistence;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetOnboardingGraph() throws IOException, IllegalAccessException {
    ServiceLevelIndicatorDTO serviceLevelIndicatorDTO =
        builderFactory.getThresholdServiceLevelIndicatorDTOBuilder().build();
    String monitoredServiceIdentifier = "monitoredServiceIdentifier";
    CVConfig cvConfig = builderFactory.appDynamicsCVConfigBuilder()
                            .identifier(HealthSourceService.getNameSpacedIdentifier(
                                monitoredServiceIdentifier, serviceLevelIndicatorDTO.getHealthSourceRef()))
                            .build();
    hPersistence.save(cvConfig);

    String textLoad = Resources.toString(
        AppDynamicsServiceimplTest.class.getResource("/timeseries/appd_metric_data_validation.json"), Charsets.UTF_8);
    OnboardingResponseDTO onboardingResponseDTO = JsonUtils.asObject(textLoad, OnboardingResponseDTO.class);

    OnboardingService mockOnboardingService = mock(OnboardingService.class);
    FieldUtils.writeField(serviceLevelIndicatorService, "onboardingService", mockOnboardingService, true);
    when(mockOnboardingService.getOnboardingResponse(eq(builderFactory.getContext().getAccountId()), any()))
        .thenReturn(JsonUtils.asObject(textLoad, OnboardingResponseDTO.class));

    String tracingId = "tracingId";
    SLIOnboardingGraphs sliOnboardingGraphs =
        serviceLevelIndicatorService.getOnboardingGraphs(builderFactory.getContext().getProjectParams(),
            monitoredServiceIdentifier, serviceLevelIndicatorDTO, tracingId);

    assertThat(sliOnboardingGraphs.getSliGraph().getStartTime()).isEqualTo(1595760600000L);
    assertThat(sliOnboardingGraphs.getSliGraph().getEndTime()).isEqualTo(1595847000000L);
    assertThat(sliOnboardingGraphs.getSliGraph().getDataPoints().get(1).getValue()).isEqualTo(50.0);
    assertThat(sliOnboardingGraphs.getSliGraph().getDataPoints().get(1).getTimeStamp()).isEqualTo(1595760660000L);
    assertThat(sliOnboardingGraphs.getMetricGraphs()).hasSize(1);
    assertThat(sliOnboardingGraphs.getMetricGraphs().get("Calls per Minute").getMetricName())
        .isEqualTo("Calls per Minute");
    assertThat(sliOnboardingGraphs.getMetricGraphs().get("Calls per Minute").getMetricIdentifier())
        .isEqualTo("Calls per Minute");
    assertThat(sliOnboardingGraphs.getMetricGraphs().get("Calls per Minute").getDataPoints().get(0).getValue())
        .isEqualTo(343.0);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreateThreshold_success() {
    ServiceLevelIndicatorDTO serviceLevelIndicatorDTO = createServiceLevelIndicator(SLIMetricType.THRESHOLD);
    ProjectParams projectParams = builderFactory.getProjectParams();
    List<String> serviceLevelIndicatorIdentifiers = serviceLevelIndicatorService.create(projectParams,
        Collections.singletonList(serviceLevelIndicatorDTO), generateUuid(), generateUuid(), generateUuid());
    List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList =
        serviceLevelIndicatorService.get(projectParams, serviceLevelIndicatorIdentifiers);
    assertThat(Collections.singletonList(serviceLevelIndicatorDTO)).isEqualTo(serviceLevelIndicatorDTOList);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreateRatio_success() {
    ServiceLevelIndicatorDTO serviceLevelIndicatorDTO = createServiceLevelIndicator(SLIMetricType.RATIO);
    ProjectParams projectParams = builderFactory.getProjectParams();
    List<String> serviceLevelIndicatorIdentifiers = serviceLevelIndicatorService.create(projectParams,
        Collections.singletonList(serviceLevelIndicatorDTO), generateUuid(), generateUuid(), generateUuid());
    List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList =
        serviceLevelIndicatorService.get(projectParams, serviceLevelIndicatorIdentifiers);
    assertThat(Collections.singletonList(serviceLevelIndicatorDTO)).isEqualTo(serviceLevelIndicatorDTOList);
  }

  private ServiceLevelIndicatorDTO createServiceLevelIndicator(SLIMetricType sliMetricType) {
    if (SLIMetricType.RATIO.equals(sliMetricType)) {
      return createRatioServiceLevelIndicator();
    } else {
      return createThresholdServiceLevelIndicator();
    }
  }

  private ServiceLevelIndicatorDTO createRatioServiceLevelIndicator() {
    return ServiceLevelIndicatorDTO.builder()
        .identifier("sliIndicator")
        .name("sliName")
        .type(ServiceLevelIndicatorType.LATENCY)
        .spec(ServiceLevelIndicatorSpec.builder()
                  .type(SLIMetricType.RATIO)
                  .spec(RatioSLIMetricSpec.builder()
                            .eventType(RatioSLIMetricEventType.GOOD)
                            .metric1("metric1")
                            .metric2("metric2")
                            .build())
                  .build())
        .build();
  }

  private ServiceLevelIndicatorDTO createThresholdServiceLevelIndicator() {
    return ServiceLevelIndicatorDTO.builder()
        .identifier("sliIndicator")
        .name("sliName")
        .type(ServiceLevelIndicatorType.LATENCY)
        .spec(ServiceLevelIndicatorSpec.builder()
                  .type(SLIMetricType.THRESHOLD)
                  .spec(ThresholdSLIMetricSpec.builder()
                            .metric1("metric1")
                            .thresholdValue(50.0)
                            .thresholdType(ThresholdType.GREATER_THAN)
                            .build())
                  .build())
        .build();
  }
}
