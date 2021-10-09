package io.harness.cvng.dashboard.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;
import io.harness.cvng.dashboard.beans.ServiceDependencyGraphDTO;
import io.harness.cvng.dashboard.beans.ServiceDependencyGraphDTO.Edge;
import io.harness.cvng.dashboard.beans.ServiceDependencyGraphDTO.ServiceSummaryDetails;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.dashboard.services.api.ServiceDependencyGraphService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CV)
public class ServiceDependencyGraphServiceImpl implements ServiceDependencyGraphService {
  @Inject private ServiceDependencyService serviceDependencyService;
  @Inject private HeatMapService heatMapService;
  @Inject private MonitoredServiceService monitoredServiceService;

  @Override
  public ServiceDependencyGraphDTO getDependencyGraph(@NonNull ProjectParams projectParams,
      @Nullable String serviceIdentifier, @Nullable String environmentIdentifier) {
    List<MonitoredService> monitoredServices =
        monitoredServiceService.list(projectParams, serviceIdentifier, environmentIdentifier);
    Set<String> identifiers =
        monitoredServices.stream().map(MonitoredService::getIdentifier).collect(Collectors.toSet());
    List<ServiceDependency> serviceDependencies =
        serviceDependencyService.getServiceDependencies(projectParams, new ArrayList<>(identifiers));

    // Get nodes for dependent services
    serviceDependencies.forEach(
        serviceDependency -> identifiers.add(serviceDependency.getFromMonitoredServiceIdentifier()));
    monitoredServices = monitoredServiceService.list(projectParams, new ArrayList<>(identifiers));

    List<HeatMap> heatMaps = heatMapService.getLatestHeatMaps(projectParams, serviceIdentifier, environmentIdentifier);

    return constructGraph(monitoredServices, serviceDependencies, heatMaps);
  }

  private ServiceDependencyGraphDTO constructGraph(
      List<MonitoredService> monitoredServices, List<ServiceDependency> serviceDependencies, List<HeatMap> heatMaps) {
    Map<GraphKey, MonitoredService> monitoredServiceMap = monitoredServices.stream().collect(
        Collectors.toMap(x -> new GraphKey(x.getServiceIdentifier(), x.getEnvironmentIdentifier()), x -> x));
    Map<GraphKey, List<HeatMap>> heatMapMap = heatMaps.stream().collect(
        Collectors.groupingBy(x -> new GraphKey(x.getServiceIdentifier(), x.getEnvIdentifier())));

    List<ServiceSummaryDetails> nodes = new ArrayList<>();
    monitoredServiceMap.forEach((key, value) -> {
      double riskScore = -1;
      long anomalousMetricsCount = 0;
      long anomalousLogsCount = 0;
      long changeCount = 0;

      if (heatMapMap.containsKey(key)) {
        List<HeatMap> serviceRisks = heatMapMap.get(key);
        riskScore = serviceRisks.stream()
                        .mapToDouble(x -> x.getHeatMapRisks().iterator().next().getRiskScore())
                        .max()
                        .orElse(-1.0);
        anomalousMetricsCount = serviceRisks.stream()
                                    .mapToLong(x -> x.getHeatMapRisks().iterator().next().getAnomalousMetricsCount())
                                    .sum();
        anomalousLogsCount =
            serviceRisks.stream().mapToLong(x -> x.getHeatMapRisks().iterator().next().getAnomalousLogsCount()).sum();
      }
      Risk risk = Risk.getRiskFromRiskScore(riskScore);
      nodes.add(ServiceSummaryDetails.builder()
                    .identifierRef(value.getIdentifier())
                    .serviceRef(value.getServiceIdentifier())
                    .environmentRef(value.getEnvironmentIdentifier())
                    .riskScore(riskScore)
                    .riskLevel(risk)
                    .anomalousMetricsCount(anomalousMetricsCount)
                    .anomalousLogsCount(anomalousLogsCount)
                    .changeCount(changeCount)
                    .build());
    });

    Set<Edge> edges =
        serviceDependencies.stream()
            .map(x -> new Edge(x.getFromMonitoredServiceIdentifier(), x.getToMonitoredServiceIdentifier()))
            .collect(Collectors.toSet());
    return ServiceDependencyGraphDTO.builder().nodes(nodes).edges(new ArrayList<>(edges)).build();
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  private static class GraphKey {
    private final String serviceIdentifier;
    private final String envIdentifier;
  }
}
