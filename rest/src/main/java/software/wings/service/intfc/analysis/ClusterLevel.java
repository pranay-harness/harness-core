package software.wings.service.intfc.analysis;

/**
 * Created by rsingh on 8/11/17.
 */
public enum ClusterLevel {
  L0(0),
  L1(1),
  L2(2);

  private final int level;

  ClusterLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }
}
