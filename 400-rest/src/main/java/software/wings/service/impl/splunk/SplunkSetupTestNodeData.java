/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.splunk;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 08/31/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SplunkSetupTestNodeData extends SetupTestNodeData {
  @NotNull private String query;
  private String hostNameField;
  private boolean isAdvancedQuery;

  @Builder
  public SplunkSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, long fromTime, long toTime, String query,
      String hostNameField, String guid, boolean isAdvancedQuery) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.SPLUNKV2, fromTime, toTime);
    this.query = query;
    this.hostNameField = hostNameField;
    this.isAdvancedQuery = isAdvancedQuery;
  }

  public void setIsAdvancedQuery(boolean isAdvancedQuery) {
    this.isAdvancedQuery = isAdvancedQuery;
  }
}
