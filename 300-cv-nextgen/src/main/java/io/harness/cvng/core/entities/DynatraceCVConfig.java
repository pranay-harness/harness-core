package io.harness.cvng.core.entities;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.HealthSourceQueryType;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DynatraceHealthSourceSpec;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.utils.analysisinfo.DevelopmentVerificationTransformer;
import io.harness.cvng.core.utils.analysisinfo.LiveMonitoringTransformer;
import io.harness.cvng.core.utils.analysisinfo.SLIMetricTransformer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("DYNATRACE")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "DynatraceCVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DynatraceCVConfig extends MetricCVConfig {
  private String serviceName;
  private String serviceEntityId;
  private String groupName;
  private List<DynatraceMetricInfo> metricInfos;
  private HealthSourceQueryType queryType;
  private List<String> serviceMethodIds;

  @Override
  protected void validateParams() {}

  @Override
  public DataSourceType getType() {
    return DataSourceType.DYNATRACE;
  }

  @Override
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }


  public void populateFromMetricDefinitions(
          List<DynatraceHealthSourceSpec.DynatraceMetricDefinition> metricDefinitions, CVMonitoringCategory category) {
    MetricPack metricPack = MetricPack.builder()
            .category(category)
            .accountId(getAccountId())
            .dataSourceType(DataSourceType.DYNATRACE)
            .projectIdentifier(getProjectIdentifier())
            .orgIdentifier(getOrgIdentifier())
            .identifier(CVNextGenConstants.CUSTOM_PACK_IDENTIFIER)
            .category(category)
            .build();
    if (this.metricInfos == null) {
      this.metricInfos = new ArrayList<>();
    }

    metricDefinitions.stream().filter(md -> md.getGroupName().equals(getGroupName())).forEach(md -> {
      DynatraceMetricInfo metricInfo =
              DynatraceMetricInfo.builder()
                      .identifier(md.getIdentifier())
                      .metricName(md.getMetricName())
                      .sli(SLIMetricTransformer.transformDTOtoEntity(md.getSli()))
                      .liveMonitoring(LiveMonitoringTransformer.transformDTOtoEntity(md.getAnalysis()))
                      .deploymentVerification(DevelopmentVerificationTransformer.transformDTOtoEntity(md.getAnalysis()))
                      .metricType(md.getRiskProfile().getMetricType())
                      .build();
      this.metricInfos.add(metricInfo);
      Set<TimeSeriesThreshold> thresholds = getThresholdsToCreateOnSaveForCustomProviders(
              metricInfo.getMetricName(), metricInfo.getMetricType(), md.getRiskProfile().getThresholdTypes());

      metricPack.addToMetrics(MetricPack.MetricDefinition.builder()
              .thresholds(new ArrayList<>(thresholds))
              .type(metricInfo.getMetricType())
              .name(metricInfo.getMetricName())
              .included(true)
              .build());
    });
    this.setMetricPack(metricPack);
  }



  public static class DynatraceCVConfigUpdatableEntity
      extends MetricCVConfigUpdatableEntity<DynatraceCVConfig, DynatraceCVConfig> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<DynatraceCVConfig> updateOperations, DynatraceCVConfig dynatraceCVConfig) {
      setCommonOperations(updateOperations, dynatraceCVConfig);
      updateOperations.set(DynatraceCVConfigKeys.serviceName, dynatraceCVConfig.getServiceName())
          .set(DynatraceCVConfigKeys.serviceEntityId, dynatraceCVConfig.getServiceEntityId());
    }
  }

  @Value
  @SuperBuilder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class DynatraceMetricInfo extends AnalysisInfo {
    String metricName;
    TimeSeriesMetricType metricType;
    MetricResponseMapping responseMapping;
  }
}
