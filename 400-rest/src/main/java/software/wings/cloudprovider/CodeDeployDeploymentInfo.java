/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.cloudprovider;

import io.harness.logging.CommandExecutionStatus;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 6/23/17.
 */
@Data
@NoArgsConstructor
public class CodeDeployDeploymentInfo {
  private CommandExecutionStatus status;
  private List<Instance> instances;
  private String deploymentId;
}
