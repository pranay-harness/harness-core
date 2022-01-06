/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogMLFeedback {
  private String appId;
  private String stateExecutionId;
  private AnalysisServiceImpl.CLUSTER_TYPE clusterType;
  private int clusterLabel;
  private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;
  private String comment;
  private String logMLFeedbackId;
  private long analysisMinute;
  private String serviceId;
  private String envId;
}
