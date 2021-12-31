package io.harness.cvng.core.utils.monitoredService;

import com.google.common.base.Preconditions;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.beans.RiskProfile;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DynatraceHealthSourceSpec;
import io.harness.cvng.core.entities.DynatraceCVConfig;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class DynatraceHealthSourceSpecTransformer
    implements CVConfigToHealthSourceTransformer<DynatraceCVConfig, DynatraceHealthSourceSpec> {
  @Override
  public DynatraceHealthSourceSpec transformToHealthSourceConfig(List<DynatraceCVConfig> cvConfigs) {
    Preconditions.checkArgument(cvConfigs.stream().map(DynatraceCVConfig::getServiceName).distinct().count() == 1,
            "Dynatrace serviceName should be same for list of all configs.");
    Preconditions.checkArgument(
            cvConfigs.stream().map(DynatraceCVConfig::getConnectorIdentifier).distinct().count() == 1,
            "ConnectorRef should be same for list of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(DynatraceCVConfig::getServiceEntityId).distinct().count() == 1,
            "Dynatrace serviceEntityId should be same for list of all configs.");
    Preconditions.checkArgument(cvConfigs.stream().map(DynatraceCVConfig::getProductName).distinct().count() == 1,
            "Application feature name should be same for list of all configs.");

    List<DynatraceHealthSourceSpec.DynatraceMetricDefinition> metricDefinitions =
            cvConfigs.stream()
                    .flatMap(cv -> CollectionUtils.emptyIfNull(cv.getMetricInfos()).stream().map(metricInfo -> {
                      RiskProfile riskProfile = RiskProfile.builder()
                              .category(cv.getMetricPack().getCategory())
                              .metricType(metricInfo.getMetricType())
                              .thresholdTypes(cv.getThresholdTypeOfMetric(metricInfo.getMetricName(), cv))
                              .build();

                      // TODO slobodanpavic check query prop
                      return DynatraceHealthSourceSpec.DynatraceMetricDefinition.builder()
                              .identifier(metricInfo.getIdentifier())
                              .metricName(metricInfo.getMetricName())
                              .riskProfile(riskProfile)
                              .sli(HealthSourceMetricDefinition.SLIDTO.builder().enabled(metricInfo.getSli().isEnabled()).build())
                              .analysis(
                                      HealthSourceMetricDefinition.AnalysisDTO.builder()
                                              .liveMonitoring(
                                                      HealthSourceMetricDefinition.AnalysisDTO.LiveMonitoringDTO.builder().enabled(metricInfo.getLiveMonitoring().isEnabled()).build())
                                              .deploymentVerification(
                                                      HealthSourceMetricDefinition.AnalysisDTO.DeploymentVerificationDTO.builder()
                                                              .enabled(metricInfo.getDeploymentVerification().isEnabled())
                                                              .serviceInstanceMetricPath(
                                                                      metricInfo.getDeploymentVerification().getServiceInstanceMetricPath())
                                                              .build())
                                              .riskProfile(riskProfile)
                                              .build())
                              .groupName(cv.getGroupName())
                              .build();
                    }))
                    .collect(Collectors.toList());
    return DynatraceHealthSourceSpec.builder()
            .connectorRef(cvConfigs.get(0).getConnectorIdentifier())
            .serviceId(cvConfigs.get(0).getServiceEntityId())
            .serviceMethodIds(cvConfigs.get(0).getServiceMethodIds())
            .feature(cvConfigs.get(0).getProductName())
            .serviceName(cvConfigs.get(0).getServiceName())
            .metricPacks(cvConfigs.stream()
                    .filter(cv -> CollectionUtils.isEmpty(cv.getMetricInfos()))
                    .map(cv -> MetricPackDTO.toMetricPackDTO(cv.getMetricPack()))
                    .collect(Collectors.toSet()))
            .metricDefinitions(metricDefinitions)
            .build();
  }
}
