/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {
  private String query;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String serviceId;
  private Set<String> nodes;
  private long logCollectionMinute;
  private boolean isExperimental;
}
