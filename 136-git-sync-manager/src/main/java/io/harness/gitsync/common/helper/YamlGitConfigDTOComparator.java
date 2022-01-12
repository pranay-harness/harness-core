/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
