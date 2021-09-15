/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;

import java.util.Comparator;
import java.util.List;

@OwnedBy(DX)
public class YamlGitConfigDTOComparator implements Comparator<YamlGitConfigDTO> {
  List<String> orderingIds;

  public YamlGitConfigDTOComparator(List<String> orderingIds) {
    this.orderingIds = orderingIds;
  }

  @Override
  public int compare(YamlGitConfigDTO o1, YamlGitConfigDTO o2) {
    Integer i1 = orderingIds.indexOf(o1.getIdentifier());
    Integer i2 = orderingIds.indexOf(o2.getIdentifier());
    return i1.compareTo(i2);
  }
}
