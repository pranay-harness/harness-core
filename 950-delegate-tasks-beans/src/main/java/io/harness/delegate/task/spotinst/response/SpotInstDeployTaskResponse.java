/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.spotinst.response;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotInstDeployTaskResponse implements SpotInstTaskResponse {
  private List<Instance> ec2InstancesAdded;
  private List<Instance> ec2InstancesExisting;
}
