package software.wings.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.Comparator;

@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
public class BuildDetailsComparator implements Comparator<BuildDetails> {
  @SuppressFBWarnings("RV_NEGATING_RESULT_OF_COMPARETO")
  @Override
  public int compare(BuildDetails bd1, BuildDetails bd2) {
    return -(ComparatorUtil.compare(bd1.getNumber(), bd2.getNumber()));
  }
}
