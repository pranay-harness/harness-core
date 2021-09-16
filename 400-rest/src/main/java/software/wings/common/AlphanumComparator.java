package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.artifact.ComparatorUtils;

import java.io.Serializable;
import java.util.Comparator;

@OwnedBy(CDC)
@TargetModule(HarnessModule._960_API_SERVICES)
public class AlphanumComparator implements Comparator<String>, Serializable {
  @Override
  public int compare(String s1, String s2) {
    return ComparatorUtils.compare(s1, s2);
  }
}
