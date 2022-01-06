/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;

@Value
@Builder
@OwnedBy(DX)
@FieldNameConstants(innerTypeName = "ServiceInfoSummaryKeys")
public class ServiceInfoSummary {
  String envId;
  String lastArtifactBuildNum;
  String serviceName;
  String lastWorkflowExecutionId;
  String lastWorkflowExecutionName;
  String infraMappingId;
  String infraMappingName;

  @UtilityClass
  public static final class ServiceInfoSummaryKeys {
    public static final String envId = "envId";
    public static final String lastArtifactBuildNum = "lastArtifactBuildNum";
    public static final String serviceName = "serviceName";
    public static final String lastWorkflowExecutionId = "lastWorkflowExecutionId";
    public static final String lastWorkflowExecutionName = "lastWorkflowExecutionName";
    public static final String infraMappingId = "infraMappingId";
    public static final String infraMappingName = "infraMappingName";
  }
}
