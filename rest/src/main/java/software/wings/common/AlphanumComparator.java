package software.wings.common;

import java.util.Comparator;

public class AlphanumComparator implements Comparator<String> {
  public int compare(String s1, String s2) {
    return ComparatorUtil.compare(s1, s2);
  }
}