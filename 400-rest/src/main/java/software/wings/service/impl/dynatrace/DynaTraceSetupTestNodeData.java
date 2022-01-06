/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.dynatrace;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 09/12/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DynaTraceSetupTestNodeData extends SetupTestNodeData {
  private String serviceMethods;
  private String serviceEntityId;

  @Builder
  public DynaTraceSetupTestNodeData(String appId, String settingId, String instanceName, boolean isServiceLevel,
      Instance instanceElement, String hostExpression, String workflowId, long fromTime, long toTime,
      String serviceMethods, String guid, String serviceEntityId) {
    super(appId, settingId, instanceName, isServiceLevel, instanceElement, hostExpression, workflowId, guid,
        StateType.DYNA_TRACE, fromTime, toTime);
    this.serviceMethods = serviceMethods;
    this.serviceEntityId = serviceEntityId;
  }
}
