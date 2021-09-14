/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.service.support.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AwsCrossAccountAttributes;

import java.util.List;

@OwnedBy(CE)
public interface AwsEKSHelperService {
  List<String> listEKSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes);

  boolean verifyAccess(String region, AwsCrossAccountAttributes awsCrossAccountAttributes);
}
