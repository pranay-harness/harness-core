/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.verification.log;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import software.wings.sm.states.CustomLogVerificationState.LogCollectionInfo;
import software.wings.sm.states.CustomLogVerificationState.Method;
import software.wings.verification.CVConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "CustomLogCVServiceConfigurationKeys")
@EqualsAndHashCode(callSuper = true)
public class CustomLogCVServiceConfiguration extends LogsCVConfiguration {
  private LogCollectionInfo logCollectionInfo;

  public void setLogCollectionInfo(LogCollectionInfo info) {
    this.logCollectionInfo = info;
    this.query = logCollectionInfo.getCollectionUrl();
  }

  public void setQuery() {
    if (logCollectionInfo != null) {
      this.query = logCollectionInfo.getCollectionUrl();
    }
  }

  public boolean validateConfiguration() {
    if (logCollectionInfo != null) {
      if (logCollectionInfo.getMethod() == Method.POST && isEmpty(logCollectionInfo.getCollectionBody())) {
        return false;
      }
      boolean bodyContainsStartTime = isNotEmpty(logCollectionInfo.getCollectionBody())
          && (logCollectionInfo.getCollectionBody().contains("${start_time}")
              || logCollectionInfo.getCollectionBody().contains("${start_time_seconds}"));
      boolean urlContainsStartTime = logCollectionInfo.getCollectionUrl().contains("${start_time}")
          || logCollectionInfo.getCollectionUrl().contains("${start_time_seconds}");
      boolean bodyContainsEndTime = isNotEmpty(logCollectionInfo.getCollectionBody())
          && (logCollectionInfo.getCollectionBody().contains("${end_time}")
              || logCollectionInfo.getCollectionBody().contains("${end_time_seconds}"));
      boolean urlContainsEndTime = logCollectionInfo.getCollectionUrl().contains("${end_time}")
          || logCollectionInfo.getCollectionUrl().contains("${end_time_seconds}");

      if ((bodyContainsEndTime || urlContainsEndTime) && (bodyContainsStartTime || urlContainsStartTime)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public CVConfiguration deepCopy() {
    CustomLogCVServiceConfiguration clonedConfig = new CustomLogCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setLogCollectionInfo(this.getLogCollectionInfo());
    clonedConfig.setQuery(this.getQuery());
    return clonedConfig;
  }

  @Data
  @Builder
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class CustomLogsCVConfigurationYaml extends LogsCVConfigurationYaml {
    private LogCollectionInfo logCollectionInfo;
  }
}
