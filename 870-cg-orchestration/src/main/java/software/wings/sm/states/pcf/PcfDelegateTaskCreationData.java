/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;

import software.wings.beans.TaskType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class PcfDelegateTaskCreationData {
  private String accountId;
  private String appId;
  private String serviceId;
  private TaskType taskType;
  private String waitId;
  private String envId;
  private EnvironmentType environmentType;
  private String infrastructureMappingId;
  private Object[] parameters;
  private long timeout;
  private List<String> tagList;
  private String serviceTemplateId;
  private boolean selectionLogsTrackingEnabled;
  private String taskDescription;
}
