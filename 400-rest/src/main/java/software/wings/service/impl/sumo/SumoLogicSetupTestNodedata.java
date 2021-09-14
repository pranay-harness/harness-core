/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.sumo;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 08/23/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SumoLogicSetupTestNodedata extends SetupTestNodeData {
  private String query;
  private String hostNameField;

  @Builder
  SumoLogicSetupTestNodedata(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, long fromTime, long toTime, String query,
      String hostNameField, String guid) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.SUMO, fromTime, toTime);
    this.query = query;
    this.hostNameField = hostNameField;
  }

  public String getHostNameField() {
    if (hostNameField == null) {
      return null;
    }
    switch (hostNameField) {
      case "_sourceHost":
        return hostNameField.toLowerCase();
      case "_sourceName":
        return hostNameField.toLowerCase();
      default:
        return hostNameField;
    }
  }
}
