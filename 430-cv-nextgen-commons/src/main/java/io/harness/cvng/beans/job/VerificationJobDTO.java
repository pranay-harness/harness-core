/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans.job;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataSourceType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants(innerTypeName = "VerificationJobDTOKeys")
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@SuperBuilder
@OwnedBy(HarnessTeam.CV)
public abstract class VerificationJobDTO {
  public static final String RUNTIME_PARAMS_STRING = "<+input>";
  private String identifier;
  private String jobName;
  private String serviceIdentifier;
  private String serviceName;
  private String envIdentifier;
  private String envName;
  private String projectIdentifier;
  private String orgIdentifier;
  private String activitySourceIdentifier;
  private List<DataSourceType> dataSources;
  private List<String> monitoringSources;
  private String verificationJobUrl;
  // TODO: make it Duration and write a custom serializer
  private String duration;
  private boolean isDefaultJob;
  private boolean allMonitoringSourcesEnabled;
  public abstract VerificationJobType getType();

  public static boolean isRuntimeParam(String value) {
    return isNotEmpty(value) && value.equals(RUNTIME_PARAMS_STRING);
  }
}
