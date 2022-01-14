package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ComparatorUtils;

import software.wings.beans.artifact.Artifact;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares build number in descending order
 */
@OwnedBy(CDC)
public class ArtifactComparator implements Comparator<Artifact>, Serializable {
  @Override
  public int compare(Artifact artifact1, Artifact artifact2) {
    return ComparatorUtils.compareDescending(artifact1.getBuildNo(), artifact2.getBuildNo());
  }
}
