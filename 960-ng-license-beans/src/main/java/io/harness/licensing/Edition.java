/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.GTM)
public enum Edition {
  COMMUNITY, // Community is exclusively on prem
  FREE,
  TEAM,
  ENTERPRISE;

  public static List<Edition> getSuperiorEdition(Edition edition) {
    List<Edition> editions = new ArrayList<>();
    for (Edition temp : Edition.values()) {
      if (edition.compareTo(temp) < 0) {
        editions.add(temp);
      }
    }
    return editions;
  }

  public static List<Edition> getSaasEditions() {
    return Lists.newArrayList(FREE, TEAM, ENTERPRISE);
  }
}
