package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition.AnalysisDTO;
import io.harness.cvng.core.beans.HealthSourceMetricDefinition.SLIDTO;
import io.harness.cvng.core.beans.HealthSourceQueryType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.exception.InvalidRequestException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "CustomHealthCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomHealthCVConfig extends CVConfig {
  String groupName;
  List<MetricDefinition> metricDefinitions;

  @Data
  @SuperBuilder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @FieldNameConstants(innerTypeName = "CustomHealthMetricDefinitionKeys")
  public static class MetricDefinition extends HealthSourceMetricDefinition {
    HealthSourceQueryType queryType;
    String urlPath;
    String requestBody;
    CustomHealthMethod method;

    String timestampFieldPathString;
    String timestampFormat;
    String metricValueFieldPathString;
    String serviceInstance;
  }

  @Override
  public boolean queueAnalysisForPreDeploymentTask() {
    return false;
  }

  @Override
  public String getDataCollectionDsl() {
    return null;
  }

  @Override
  public TimeRange getFirstTimeDataCollectionTimeRange() {
    return null;
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.CUSTOM_HEALTH;
  }

  @Override
  protected void validateParams() {
    checkNotNull(groupName, generateErrorMessageFromParam(CustomHealthCVConfigKeys.groupName));
    checkNotNull(metricDefinitions, generateErrorMessageFromParam(CustomHealthCVConfigKeys.metricDefinitions));
    Set<String> uniqueMetricDefinitionsNames = new HashSet<>();

    for (int metricDefinitionIndex = 0; metricDefinitionIndex < metricDefinitions.size(); metricDefinitionIndex++) {
      MetricDefinition metricDefinition = metricDefinitions.get(metricDefinitionIndex);
      checkNotNull(metricDefinition.getMetricName(),
          generateErrorMessageFromParam("metricName") + " for index " + metricDefinitionIndex);
      checkNotNull(metricDefinition.method,
          generateErrorMessageFromParam(MetricDefinition.CustomHealthMetricDefinitionKeys.method) + " for index "
              + metricDefinitionIndex);
      checkNotNull(metricDefinition.urlPath,
          generateErrorMessageFromParam(MetricDefinition.CustomHealthMetricDefinitionKeys.urlPath) + " for index "
              + metricDefinitionIndex);

      AnalysisDTO analysisDTO = metricDefinition.getAnalysis();
      SLIDTO sliDTO = metricDefinition.getSli();

      switch (metricDefinition.getQueryType()) {
        case HOST_BASED:
          if ((analysisDTO != null && analysisDTO.getLiveMonitoring() != null
                  && analysisDTO.getLiveMonitoring().getEnabled() != null
                  && analysisDTO.getLiveMonitoring().getEnabled() == true)
              || (sliDTO != null && sliDTO.getEnabled() != null && sliDTO.getEnabled())) {
            throw new InvalidRequestException("Host based queries can only be used for deployment verification.");
          }
          break;
        case SERVICE_BASED:
          if (analysisDTO != null && analysisDTO.getDeploymentVerification() != null
              && analysisDTO.getDeploymentVerification().getEnabled() != null
              && analysisDTO.getDeploymentVerification().getEnabled()) {
            throw new InvalidRequestException(
                "Service based queries can only be used for live monitoring and service level indicators.");
          }
          break;
        default:
          throw new InvalidRequestException(String.format(
              "Invalid query type %s provided, must be SERVICE_BASED or HOST_BASED", metricDefinition.queryType));
      }

      String uniqueKey = getMetricAndGroupNameKey(groupName, metricDefinition.getMetricName());
      if (uniqueMetricDefinitionsNames.contains(uniqueKey)) {
        throw new InvalidRequestException(
            String.format("Duplicate group name (%s) and metric name (%s) combination present.", groupName,
                metricDefinition.getMetricName()));
      }
      uniqueMetricDefinitionsNames.add(uniqueKey);
    }
  }

  private String getMetricAndGroupNameKey(String groupName, String metricName) {
    return String.format("%s%s", groupName, metricName);
  }
}
