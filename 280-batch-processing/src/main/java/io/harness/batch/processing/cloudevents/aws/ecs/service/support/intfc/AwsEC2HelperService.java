/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import java.util.Set;

public interface AwsEC2HelperService {
  List<Instance> listEc2Instances(
      AwsCrossAccountAttributes awsCrossAccountAttributes, Set<String> instanceIds, String region);
}
