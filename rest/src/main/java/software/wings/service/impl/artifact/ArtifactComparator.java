package software.wings.service.impl.artifact;

import software.wings.beans.artifact.Artifact;
import software.wings.common.ComparatorUtil;

import java.util.Comparator;

/**
 * Compares build number in descending order
 */
public class ArtifactComparator implements Comparator<Artifact> {
  public int compare(Artifact artifact1, Artifact artifact2) {
    return -(ComparatorUtil.compare(artifact1.getBuildNo(), artifact2.getBuildNo()));
  }
}