/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
