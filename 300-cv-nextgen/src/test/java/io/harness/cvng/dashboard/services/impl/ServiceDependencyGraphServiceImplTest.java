package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.utils.DateTimeUtils.roundDownTo5MinBoundary;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIVE_MIN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.BuilderFactory.Context;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.Sources;
import io.harness.cvng.core.beans.monitoredService.RiskData;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.dashboard.beans.ServiceDependencyGraphDTO;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.dashboard.services.api.ServiceDependencyGraphService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class ServiceDependencyGraphServiceImplTest extends CvNextGenTestBase {
  @Inject private ServiceDependencyGraphService serviceDependencyGraphService;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private HeatMapService heatMapService;
  @Inject private HPersistence hPersistence;
  @Mock private NextGenService nextGenService;

  private BuilderFactory builderFactory;
  private Context context;
  private Clock clock;

  @Before
  public void setUp() throws Exception {
    builderFactory = BuilderFactory.getDefault();
    context = builderFactory.getContext();
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);

    FieldUtils.writeField(heatMapService, "clock", clock, true);
    FieldUtils.writeField(serviceDependencyGraphService, "heatMapService", heatMapService, true);
    FieldUtils.writeField(serviceDependencyGraphService, "nextGenService", nextGenService, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetDependencyGraph() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    MonitoredServiceDTO monitoredServiceDTO =
        builderFactory.monitoredServiceDTOBuilder().sources(Sources.builder().build()).build();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);

    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);
    heatMap =
        builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).category(CVMonitoringCategory.PERFORMANCE).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);

    ServiceDependencyGraphDTO graphDTO = serviceDependencyGraphService.getDependencyGraph(
        context.getProjectParams(), context.getServiceIdentifier(), context.getEnvIdentifier());

    assertThat(graphDTO).isNotNull();
    assertThat(graphDTO.getNodes().size()).isEqualTo(1);
    assertThat(graphDTO.getNodes().get(0).getIdentifierRef()).isEqualTo(monitoredServiceDTO.getIdentifier());
    assertThat(graphDTO.getNodes().get(0).getServiceRef()).isEqualTo(context.getServiceIdentifier());
    assertThat(graphDTO.getNodes().get(0).getEnvironmentRef()).isEqualTo(context.getEnvIdentifier());
    assertThat(graphDTO.getNodes().get(0).getRiskLevel()).isEqualTo(Risk.HEALTHY);
    assertThat(graphDTO.getNodes().get(0).getRiskData())
        .isEqualTo(RiskData.builder().riskStatus(Risk.HEALTHY).healthScore(75).build());
    assertThat(graphDTO.getNodes().get(0).getType()).isEqualTo(MonitoredServiceType.APPLICATION);
    assertThat(graphDTO.getEdges().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetDependencyGraph_forMonitoredService() {
    Instant endTime = roundDownTo5MinBoundary(clock.instant());
    MonitoredServiceDTO monitoredServiceDTO =
        builderFactory.monitoredServiceDTOBuilder().sources(Sources.builder().build()).build();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);

    MonitoredServiceDTO edgeDTO =
        builderFactory.monitoredServiceDTOBuilder().sources(Sources.builder().build()).build();
    edgeDTO.setIdentifier(monitoredServiceDTO.getDependencies().iterator().next().getMonitoredServiceIdentifier());
    edgeDTO.setServiceRef(monitoredServiceDTO.getDependencies().iterator().next().getMonitoredServiceIdentifier());
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), edgeDTO);

    HeatMap heatMap = builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);
    heatMap =
        builderFactory.heatMapBuilder().heatMapResolution(FIVE_MIN).category(CVMonitoringCategory.PERFORMANCE).build();
    setStartTimeEndTimeAndRiskScoreWith5MinBucket(heatMap, endTime, 0.15, 0.25);
    hPersistence.save(heatMap);

    ServiceDependencyGraphDTO graphDTO = serviceDependencyGraphService.getDependencyGraph(
        context.getProjectParams(), context.getServiceIdentifier(), context.getEnvIdentifier());

    assertThat(graphDTO).isNotNull();
    assertThat(graphDTO.getNodes().size()).isEqualTo(2);
    assertThat(graphDTO.getEdges().size()).isEqualTo(2);
  }

  private void setStartTimeEndTimeAndRiskScoreWith5MinBucket(
      HeatMap heatMap, Instant endTime, double firstHalfRiskScore, double secondHalfRiskScore) {
    Instant startTime = endTime.minus(4, ChronoUnit.HOURS);
    heatMap.setHeatMapBucketStartTime(startTime);
    heatMap.setHeatMapBucketEndTime(endTime);
    List<HeatMapRisk> heatMapRisks = new ArrayList<>();

    for (Instant time = startTime; time.isBefore(startTime.plus(2, ChronoUnit.HOURS));
         time = time.plus(5, ChronoUnit.MINUTES)) {
      heatMapRisks.add(HeatMapRisk.builder()
                           .riskScore(firstHalfRiskScore)
                           .startTime(time)
                           .endTime(time.plus(5, ChronoUnit.MINUTES))
                           .anomalousMetricsCount(1)
                           .anomalousLogsCount(2)
                           .build());
    }
    for (Instant time = startTime.plus(2, ChronoUnit.HOURS); time.isBefore(endTime);
         time = time.plus(5, ChronoUnit.MINUTES)) {
      heatMapRisks.add(HeatMapRisk.builder()
                           .riskScore(secondHalfRiskScore)
                           .startTime(time)
                           .endTime(time.plus(5, ChronoUnit.MINUTES))
                           .anomalousMetricsCount(1)
                           .anomalousLogsCount(2)
                           .build());
    }
    heatMap.setHeatMapRisks(heatMapRisks);
  }
}
