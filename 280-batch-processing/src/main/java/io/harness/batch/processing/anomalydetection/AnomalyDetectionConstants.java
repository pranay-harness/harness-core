package io.harness.batch.processing.anomalydetection;

public class AnomalyDetectionConstants {
  public static final int DAYS_TO_CONSIDER = 15;
  public static final double DEFAULT_COST = -1.0;
  public static final double MINIMUM_AMOUNT = 75.0;
  public static final int MIN_DAYS_REQUIRED_DAILY = 14;
  public static final int BATCH_SIZE = 50;
  public static final Double STATS_MODEL_RELATIVITY_THRESHOLD = 1.25;
  public static final Double STATS_MODEL_ABSOLUTE_THRESHOLD = 20.0;
  public static final Double STATS_MODEL_PROBABILITY_THRESHOLD = 0.98;

  private AnomalyDetectionConstants() {}
}
