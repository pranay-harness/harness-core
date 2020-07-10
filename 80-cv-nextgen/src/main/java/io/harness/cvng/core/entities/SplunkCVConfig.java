package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.util.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.beans.DataSourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@JsonTypeName("SPLUNK")
@Data
@FieldNameConstants(innerTypeName = "SplunkCVConfigKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SplunkCVConfig extends LogCVConfig {
  @VisibleForTesting static final String DSL = readDSL();
  private String serviceInstanceIdentifier;

  @Override
  public DataSourceType getType() {
    return DataSourceType.SPLUNK;
  }

  @Override
  protected void validateParams() {
    checkNotNull(
        serviceInstanceIdentifier, generateErrorMessageFromParam(SplunkCVConfigKeys.serviceInstanceIdentifier));
  }

  @JsonIgnore
  @Override
  public String getDataCollectionDsl() {
    // TODO: Need to define ownership of DSL properly. Currently for metric it is with Metric Pack and for log there is
    // no such concept.
    return DSL;
  }

  private static String readDSL() {
    try {
      return Resources.toString(SplunkCVConfig.class.getResource("splunk.datacollection"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
