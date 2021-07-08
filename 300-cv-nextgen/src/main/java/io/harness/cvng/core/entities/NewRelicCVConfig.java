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

@JsonTypeName("NEW_RELIC")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "NewRelicCVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NewRelicCVConfig extends MetricCVConfig {
  private String applicationName;
  private long applicationId;

  @Override
  public DataSourceType getType() {
    return DataSourceType.NEW_RELIC;
  }

  @Override
  @JsonIgnore
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }

  @Override
  protected void validateParams() {
    checkNotNull(applicationName, generateErrorMessageFromParam(NewRelicCVConfigKeys.applicationName));
    checkNotNull(applicationId, generateErrorMessageFromParam(NewRelicCVConfigKeys.applicationId));
  }

  public static class NewRelicCVConfigUpdatableEntity
      extends MetricCVConfigUpdatableEntity<NewRelicCVConfig, NewRelicCVConfig> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<NewRelicCVConfig> updateOperations, NewRelicCVConfig newRelicCVConfig) {
      setCommonOperations(updateOperations, newRelicCVConfig);
      updateOperations.set(NewRelicCVConfigKeys.applicationName, newRelicCVConfig.getApplicationName())
          .set(NewRelicCVConfigKeys.applicationId, newRelicCVConfig.getApplicationId());
    }
  }
}
