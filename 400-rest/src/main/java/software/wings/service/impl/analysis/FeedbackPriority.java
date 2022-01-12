/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

public enum FeedbackPriority {
  BASELINE(0.0),
  P5(0.1),
  P4(0.2),
  P3(0.4),
  P2(0.6),
  P1(0.8),
  P0(1.0);
  private double score;

  FeedbackPriority(double priorityScore) {
    score = priorityScore;
  }

  public double getScore() {
    return score;
  }
}
