/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.common;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.artifact.ComparatorUtils;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.Serializable;
import java.util.Comparator;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._960_API_SERVICES)
public class BuildDetailsComparator implements Comparator<BuildDetails>, Serializable {
  @Override
  public int compare(BuildDetails bd1, BuildDetails bd2) {
    return ComparatorUtils.compareDescending(bd1.getNumber(), bd2.getNumber());
  }
}
