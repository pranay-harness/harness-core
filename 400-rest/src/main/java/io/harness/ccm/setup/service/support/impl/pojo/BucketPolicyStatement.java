/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.service.support.impl.pojo;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class BucketPolicyStatement {
  String Sid;
  String Effect;
  Map<String, List<String>> Principal;
  Object Action;
  Object Resource;
  Map<String, Map<String, String>> Condition;
}
