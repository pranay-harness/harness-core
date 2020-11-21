package software.wings.common;

import io.harness.artifact.ComparatorUtils;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.Serializable;
import java.util.Comparator;

public class BuildDetailsComparator implements Comparator<BuildDetails>, Serializable {
  @Override
  public int compare(BuildDetails bd1, BuildDetails bd2) {
    return ComparatorUtils.compareDescending(bd1.getNumber(), bd2.getNumber());
  }
}
