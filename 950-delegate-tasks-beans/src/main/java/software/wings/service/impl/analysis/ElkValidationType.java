package software.wings.service.impl.analysis;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
@OwnedBy(HarnessTeam.CV)
public enum ElkValidationType {
  PASSWORD("Password"),
  TOKEN("API Token");

  private String name;

  ElkValidationType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
