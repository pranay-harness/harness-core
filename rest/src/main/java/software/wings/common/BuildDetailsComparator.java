package software.wings.common;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.Comparator;

public class BuildDetailsComparator implements Comparator<BuildDetails> {
  @Override
  public int compare(BuildDetails bd1, BuildDetails bd2) {
    return -(ComparatorUtil.compare(bd1.getNumber(), bd2.getNumber()));
  }
}
