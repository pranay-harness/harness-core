package io.harness.cvng.servicelevelobjective;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorType;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.RatioSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceLevelIndicatorServiceImplTest extends CvNextGenTestBase {
  BuilderFactory builderFactory;
  @Inject ServiceLevelIndicatorService serviceLevelIndicatorService;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.builder().build();
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
        .spec(
            ServiceLevelIndicatorSpec.builder()
                .type(SLIMetricType.RATIO)
                .spec(RatioSLIMetricSpec.builder().eventType("eventName").metric1("metric1").metric2("metric2").build())
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
                            .thresholdType(TimeSeriesThresholdType.ACT_WHEN_LOWER)
                            .build())
                  .build())
        .build();
  }
}
