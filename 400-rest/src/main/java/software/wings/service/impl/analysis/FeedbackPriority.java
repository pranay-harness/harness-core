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
