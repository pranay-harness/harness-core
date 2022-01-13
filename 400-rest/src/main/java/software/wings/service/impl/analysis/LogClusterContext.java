/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import software.wings.sm.StateType;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by sriram_parthasarathy on 8/29/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogClusterContext {
  private String accountId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  private String stateExecutionId;
  private String serviceId;
  private Set<String> controlNodes;
  private Set<String> testNodes;
  private String query;
  private boolean isSSL;
  private int appPort;
  private StateType stateType;
  private String stateBaseUrl;
}
