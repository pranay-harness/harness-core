package software.wings.common;

import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;

public class VerificationConstants {
  private VerificationConstants() {}

  public static List<StateType> getMetricAnalysisStates() {
    return Arrays.asList(StateType.APM_VERIFICATION, StateType.APP_DYNAMICS, StateType.DATA_DOG, StateType.DYNA_TRACE,
        StateType.PROMETHEUS, StateType.NEW_RELIC);
  }

  public static List<StateType> getLogAnalysisStates() {
    return Arrays.asList(
        StateType.ELK, StateType.SUMO, StateType.LOGZ, StateType.SPLUNK, StateType.SPLUNKV2, StateType.CLOUD_WATCH);
  }
}
