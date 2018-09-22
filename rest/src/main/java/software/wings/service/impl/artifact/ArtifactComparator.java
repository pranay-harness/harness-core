package software.wings.service.impl.artifact;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.beans.artifact.Artifact;
import software.wings.common.ComparatorUtil;

import java.util.Comparator;

/**
 * Compares build number in descending order
 */
@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
public class ArtifactComparator implements Comparator<Artifact> {
  @SuppressFBWarnings("RV_NEGATING_RESULT_OF_COMPARETO")
  public int compare(Artifact artifact1, Artifact artifact2) {
    return -(ComparatorUtil.compare(artifact1.getBuildNo(), artifact2.getBuildNo()));
  }
}