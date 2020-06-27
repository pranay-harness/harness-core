package io.harness.cvng.core.beans;

import java.util.List;

public class ServiceGuardAnalysisDataDTO {
  boolean isLongTermPattern;
  long lastSeenTime;
  int risk;
  List<Double> shortTermHistory;
}
