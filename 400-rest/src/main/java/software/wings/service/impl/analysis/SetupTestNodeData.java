/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.deployment.InstanceDetails;

import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Created by rsingh on 8/3/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class SetupTestNodeData {
  @NotNull private String appId;
  @NotNull private String settingId;
  private String instanceName;
  @JsonProperty private boolean isServiceLevel;
  private Instance instanceElement;
  private String hostExpression;
  private String workflowId;
  private String guid;
  private StateType stateType;
  private long toTime = System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
  private long fromTime = toTime - TimeUnit.MINUTES.toMillis(20) / TimeUnit.SECONDS.toMillis(1);

  public SetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, String guid, StateType stateType,
      long fromTime, long toTime) {
    this.appId = appId;
    this.settingId = settingId;
    this.instanceName = instanceName;
    this.isServiceLevel = isServiceLevel;
    this.instanceElement = instanceElement;
    this.hostExpression = hostExpression;
    this.workflowId = workflowId;
    this.guid = isEmpty(guid) ? stateType.name() + workflowId + settingId : guid;
    this.stateType = stateType;
    this.toTime = toTime;
    this.fromTime = fromTime;
  }
  @Value
  @Builder
  public static class Instance {
    InstanceDetails instanceDetails;
  }
}
