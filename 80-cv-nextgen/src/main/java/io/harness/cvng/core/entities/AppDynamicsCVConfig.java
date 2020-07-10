package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.util.ErrorMessageUtils.generateErrorMessageFromParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.beans.DataSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("APP_DYNAMICS")
@Data
@FieldNameConstants(innerTypeName = "AppDynamicsCVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppDynamicsCVConfig extends MetricCVConfig {
  private long tierId;
  private long applicationId;
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
}
