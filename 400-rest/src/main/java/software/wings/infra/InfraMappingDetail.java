/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.infra;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.WorkflowExecution;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraMappingDetail {
  private InfrastructureMapping infrastructureMapping;
  private List<WorkflowExecution> workflowExecutionList;
}
