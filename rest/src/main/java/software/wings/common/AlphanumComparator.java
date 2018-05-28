package software.wings.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Comparator;

@SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
public class AlphanumComparator implements Comparator<String> {
  public int compare(String s1, String s2) {
    return ComparatorUtil.compare(s1, s2);
  }
}