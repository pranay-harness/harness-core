package software.wings.service.intfc.analysis;

/**
 * Created by rsingh on 8/11/17.
 */
public enum ClusterLevel {
  L0(0),
  L1(1),
  L2(2),
  H0(-1),
  H1(-2),
  H2(-3),
  HF(-4);

  private final int level;

  ClusterLevel(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public static ClusterLevel getHeartBeatLevel(ClusterLevel clusterLevel) {
    switch (clusterLevel) {
      case L0:
        return H0;
      case L1:
        return H1;
      case L2:
        return H2;
      default:
        throw new RuntimeException("Cluster " + clusterLevel.name() + " does not have a heartbeat");
    }
  }

  public static ClusterLevel getFinal() {
    return HF;
  }

  public ClusterLevel next() {
    switch (this) {
      case L0:
        return ClusterLevel.L1;
      case L1:
      case L2:
        return ClusterLevel.L2;
      case H0:
        return ClusterLevel.H1;
      case H1:
        return ClusterLevel.H2;
      case H2:
      case HF:
        return ClusterLevel.HF;
      default:
        throw new RuntimeException("Unknown cluster level " + level);
    }
  }
}
