package io.harness.cvng.core.entities;

import static io.harness.cvng.beans.DataSourceType.STACKDRIVER_LOG;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.DataSourceType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("STACKDRIVER_LOG")
@Data
@Builder
@FieldNameConstants(innerTypeName = "StackdriverLogCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackdriverLogCVConfig extends LogCVConfig {
  String messageIdentifier;
  String serviceInstanceIdentifier;

  @Override
  public String getHostCollectionDSL() {
    // TODO: To be done with CVNG-2626
    throw new RuntimeException("Not implemented");
  }

  @Override
  protected void validateParams() {
    checkNotNull(messageIdentifier, generateErrorMessageFromParam(StackdriverLogCVConfigKeys.messageIdentifier));
    checkNotNull(
        serviceInstanceIdentifier, generateErrorMessageFromParam(StackdriverLogCVConfigKeys.serviceInstanceIdentifier));
  }

  @Override
  public DataSourceType getType() {
    return STACKDRIVER_LOG;
  }

  @Override
  public String getDataCollectionDsl() {
    return null;
  }

  public static class StackdriverLogCVConfigUpdatableEntity
      extends LogCVConfigUpdatableEntity<StackdriverLogCVConfig, StackdriverLogCVConfig> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<StackdriverLogCVConfig> updateOperations, StackdriverLogCVConfig stackdriverLogCVConfig) {
      setCommonOperations(updateOperations, stackdriverLogCVConfig);
      updateOperations.set(StackdriverLogCVConfigKeys.messageIdentifier, stackdriverLogCVConfig.getMessageIdentifier());
      updateOperations.set(
          StackdriverLogCVConfigKeys.serviceInstanceIdentifier, stackdriverLogCVConfig.getServiceInstanceIdentifier());
    }
  }
}
