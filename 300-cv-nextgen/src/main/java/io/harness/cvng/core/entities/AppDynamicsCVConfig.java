package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.DataSourceType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
    }
  }
}
