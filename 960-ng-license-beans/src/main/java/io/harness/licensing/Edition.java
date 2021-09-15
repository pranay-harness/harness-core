package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.GTM)
public enum Edition {
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
}
