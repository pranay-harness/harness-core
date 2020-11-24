package io.harness.artifacts.comparator;

import io.harness.artifact.ComparatorUtils;
import io.harness.artifacts.beans.BuildDetailsInternal;

import java.util.Comparator;

public class BuildDetailsInternalComparatorAscending implements Comparator<BuildDetailsInternal> {
  @Override
  public int compare(BuildDetailsInternal o1, BuildDetailsInternal o2) {
    return ComparatorUtils.compare(o1.getNumber(), o2.getNumber());
  }
}
