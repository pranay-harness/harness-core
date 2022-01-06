/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.analysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 8/11/17.
 */
public enum ClusterLevel {
  L0(0, "preprocessing phase zero"),
  L1(1, "preprocessing phase one"),
  L2(2, "preprocessing phase two"),
  H0(-1, "heartbeat phase zero"),
  H1(-2, "heartbeat phase one"),
  H2(-3, "heartbeat phase two"),
  HF(-4, "final phase");
  private static final Map<Integer, ClusterLevel> CLUSTER_LEVEL_MAP = new HashMap<>();

  static {
    for (ClusterLevel clusterLevel : ClusterLevel.values()) {
      CLUSTER_LEVEL_MAP.put(clusterLevel.getLevel(), clusterLevel);
    }
  }

  private final int level;
  private final String clusteringPhaseString;
  ClusterLevel(int level, String clusteringPhaseString) {
    this.level = level;
    this.clusteringPhaseString = clusteringPhaseString;
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

      case H0:
        return H0;
      case H1:
        return H1;
      case H2:
        return H2;
      case HF:
        return HF;

      default:
        throw new RuntimeException("Cluster " + clusterLevel.name() + " does not have a heartbeat");
    }
  }

  public static ClusterLevel getFinal() {
    return HF;
  }

  public static List<ClusterLevel> getAllHeartbeatLevels() {
    return Arrays.asList(ClusterLevel.H0, ClusterLevel.H1, ClusterLevel.H2, ClusterLevel.HF);
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

  public static ClusterLevel valueOf(int level) {
    return CLUSTER_LEVEL_MAP.get(level);
  }

  /**
   * This can be used to generate human readable strings.
   */
  public String getClusteringPhase() {
    return clusteringPhaseString;
  }
}
