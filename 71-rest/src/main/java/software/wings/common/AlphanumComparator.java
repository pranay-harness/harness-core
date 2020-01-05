package software.wings.common;

import java.io.Serializable;
import java.util.Comparator;

public class AlphanumComparator implements Comparator<String>, Serializable {
  @Override
  public int compare(String s1, String s2) {
    return ComparatorUtils.compare(s1, s2);
  }
}