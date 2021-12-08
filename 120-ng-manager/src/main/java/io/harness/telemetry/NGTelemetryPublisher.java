package io.harness.telemetry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class NGTelemetryPublisher {
  @Inject TelemetryReporter telemetryReporter;

  String TOTAL_NUMBER_OF_DEPLOYMENTS = "total_number_of_deployments";
  String DEPLOYMENT_COUNT_IN_A_DAY = "deployment_count_in_a_day";

  public void recordTelemetry() {
    Long MILLISECONDS_IN_A_DAY = 86400000L;
    Long totalDeploymentCount = 0L;
    Long deploymentsInADay = 0L;

    String accountId = "";

    // Deployments tracking..

    HashMap<String, Object> map = new HashMap<>();
    map.put(DEPLOYMENT_COUNT_IN_A_DAY, deploymentsInADay);
    map.put(TOTAL_NUMBER_OF_DEPLOYMENTS, totalDeploymentCount);
    telemetryReporter.sendGroupEvent(
        accountId, null, map, null, TelemetryOption.builder().sendForCommunity(true).build());
  }
}
