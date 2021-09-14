/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "CompareEnvironmentAggregationInfoKeys")
@Data
@NoArgsConstructor
@OwnedBy(DX)
public class CompareEnvironmentAggregationInfo {
  @Id private ID _id;
  private String serviceId;
  private String serviceName;
  private String count;
  private List<ServiceInfoSummary> serviceInfoSummaries;

  @Data
  @NoArgsConstructor
  public static final class ID {
    private String serviceId;
    private String envId;
    private String lastArtifactBuildNum;
    private String lastWorkflowExecutionId;
    private String infraMappingId;
  }

  @UtilityClass
  public static final class CompareEnvironmentAggregationInfoKeys {
    public static final String serviceId = "serviceId";
    public static final String serviceName = "serviceName";
    public static final String count = "count";
    public static final String serviceInfoSummaries = "serviceInfoSummaries";
  }
}
