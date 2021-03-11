package io.harness.accesscontrol.common.filter;

import java.util.Set;

public enum ManagedFilter {
  NO_FILTER,
  ONLY_MANAGED,
  ONLY_CUSTOM;

  public static ManagedFilter buildFromSet(Set<Boolean> managedFilterSet) {
    if (managedFilterSet.contains(Boolean.TRUE) && managedFilterSet.contains(Boolean.FALSE)) {
      return ManagedFilter.NO_FILTER;
    } else if (managedFilterSet.contains(Boolean.TRUE)) {
      return ManagedFilter.ONLY_MANAGED;
    } else if (managedFilterSet.contains(Boolean.FALSE)) {
      return ManagedFilter.ONLY_CUSTOM;
    }
    return ManagedFilter.NO_FILTER;
  }
}
