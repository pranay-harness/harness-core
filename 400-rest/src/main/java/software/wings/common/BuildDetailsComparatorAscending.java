/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.artifact.ComparatorUtils;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.Serializable;
import java.util.Comparator;

@OwnedBy(CDC)
@TargetModule(HarnessModule._960_API_SERVICES)
public class BuildDetailsComparatorAscending implements Comparator<BuildDetails>, Serializable {
  @Override
  public int compare(BuildDetails bd1, BuildDetails bd2) {
    return ComparatorUtils.compare(bd1.getNumber(), bd2.getNumber());
  }
}
