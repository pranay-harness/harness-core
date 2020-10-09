package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

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
  @VisibleForTesting static final String DSL = readDSL("splunk.datacollection");
  static final String HOST_COLLECTION_DSL = readDSL("splunk_host_collection.datacollection");
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

  private static String readDSL(String fileName) {
    try {
      return Resources.toString(SplunkCVConfig.class.getResource(fileName), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getHostCollectionDSL() {
    return HOST_COLLECTION_DSL;
  }
}
