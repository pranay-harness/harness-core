/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec.AppDMetricDefinitions;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.utils.analysisinfo.DevelopmentVerificationTransformer;
import io.harness.cvng.core.utils.analysisinfo.LiveMonitoringTransformer;
import io.harness.cvng.core.utils.analysisinfo.SLIMetricTransformer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("APP_DYNAMICS")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "AppDynamicsCVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppDynamicsCVConfig extends MetricCVConfig {
  private String applicationName;
  private String tierName;
  private String groupName;
  private List<MetricInfo> metricInfos;

  @Override
  public DataSourceType getType() {
    return DataSourceType.APP_DYNAMICS;
  }

  @Override
  @JsonIgnore
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }

  @Override
  protected void validateParams() {
    checkNotNull(applicationName, generateErrorMessageFromParam(AppDynamicsCVConfigKeys.applicationName));
    checkNotNull(tierName, generateErrorMessageFromParam(AppDynamicsCVConfigKeys.tierName));
  }

  public static class AppDynamicsCVConfigUpdatableEntity
      extends MetricCVConfigUpdatableEntity<AppDynamicsCVConfig, AppDynamicsCVConfig> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<AppDynamicsCVConfig> updateOperations, AppDynamicsCVConfig appDynamicsCVConfig) {
      setCommonOperations(updateOperations, appDynamicsCVConfig);
      updateOperations.set(AppDynamicsCVConfigKeys.applicationName, appDynamicsCVConfig.getApplicationName())
          .set(AppDynamicsCVConfigKeys.tierName, appDynamicsCVConfig.getTierName());
      if (appDynamicsCVConfig.getMetricInfos() != null) {
        updateOperations.set(AppDynamicsCVConfigKeys.metricInfos, appDynamicsCVConfig.getMetricInfos());
      }
    }
  }

  public void populateFromMetricDefinitions(
      List<AppDMetricDefinitions> metricDefinitions, CVMonitoringCategory category) {
    MetricPack metricPack = MetricPack.builder()
                                .category(category)
                                .accountId(getAccountId())
                                .dataSourceType(DataSourceType.APP_DYNAMICS)
                                .projectIdentifier(getProjectIdentifier())
                                .orgIdentifier(getOrgIdentifier())
                                .identifier(CVNextGenConstants.CUSTOM_PACK_IDENTIFIER)
                                .category(category)
                                .build();
    if (this.metricInfos == null) {
      this.metricInfos = new ArrayList<>();
    }

    metricDefinitions.stream().filter(md -> md.getGroupName().equals(getGroupName())).forEach(md -> {
      MetricInfo metricInfo =
          MetricInfo.builder()
              .identifier(md.getIdentifier())
              .metricName(md.getMetricName())
              .baseFolder(md.getBaseFolder())
              .metricPath(md.getMetricPath())
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

  @Value
  @SuperBuilder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class MetricInfo extends AnalysisInfo {
    String metricName;
    String baseFolder;
    String metricPath;
    TimeSeriesMetricType metricType;
  }
}
