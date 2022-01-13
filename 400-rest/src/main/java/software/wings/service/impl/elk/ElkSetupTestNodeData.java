/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.elk;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * ELK request payload for TestNodeData.
 * Created by Pranjal on 08/17/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ElkSetupTestNodeData extends SetupTestNodeData {
  private String query;
  private String indices;
  private String messageField;
  private String timeStampField;
  private String timeStampFieldFormat;
  private ElkQueryType queryType;
  private String hostNameField;

  @Builder
  public ElkSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, long fromTime, long toTime, String query,
      String indices, String messageField, String timeStampField, String timeStampFieldFormat, ElkQueryType queryType,
      String hostNameField, String guid) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.ELK, fromTime, toTime);
    this.query = query;
    this.indices = indices;
    this.messageField = messageField;
    this.timeStampField = timeStampField;
    this.timeStampFieldFormat = timeStampFieldFormat;
    this.queryType = queryType;
    this.hostNameField = hostNameField;
  }
}
