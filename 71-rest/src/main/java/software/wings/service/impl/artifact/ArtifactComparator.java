package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.Artifact;
import software.wings.common.ComparatorUtils;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares build number in descending order
 */
@OwnedBy(CDC)
public class ArtifactComparator implements Comparator<Artifact>, Serializable {
  @Override
  public int compare(Artifact artifact1, Artifact artifact2) {
    return ComparatorUtils.compareDecending(artifact1.getBuildNo(), artifact2.getBuildNo());
  }
}