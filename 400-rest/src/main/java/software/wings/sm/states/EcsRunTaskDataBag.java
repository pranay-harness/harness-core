/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsRunTaskDataBag {
  AwsConfig awsConfig;
  String envUuid;
  String applicationAccountId;
  String applicationAppId;
  String applicationUuid;
  String ecsRunTaskFamilyName;
  List<String> listTaskDefinitionJson;
  boolean skipSteadyStateCheck;
  Long serviceSteadyStateTimeout;
}
