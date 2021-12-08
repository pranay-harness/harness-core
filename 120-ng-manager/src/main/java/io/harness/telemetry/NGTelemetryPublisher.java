package io.harness.telemetry;

import software.wings.beans.PipelineExecution.PipelineExecutionKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
public class NGTelemetryPublisher {
  @Inject TelemetryReporter telemetryReporter;
  //@Inject PMSExecutionService pmsExecutionService;

  String DEPLOYMENT_COUNT_IN_A_DAY = "deployment_count_in_a_day";
  String TOTAL_NUMBER_OF_DEPLOYMENTS = "total_number_of_deployments";

  public void recordTelemetry() {
    Long MILLISECONDS_IN_A_DAY = 86400000L;
    Long deploymentsInADay = 0L;
    Long totalDeploymentCount = 0L;

    String accountId = getAccountId();

    Criteria criteria =
        Criteria.where(PipelineExecutionKeys.createdAt).gt(System.currentTimeMillis() - MILLISECONDS_IN_A_DAY);
    // deploymentsInADay = pmsExecutionService.getCountOfExecutions(criteria);

    Criteria noCriteria = new Criteria();
    // totalDeploymentCount = pmsExecutionService.getCountOfExecutions(noCriteria);

    HashMap<String, Object> map = new HashMap<>();
    map.put(DEPLOYMENT_COUNT_IN_A_DAY, deploymentsInADay);
    map.put(TOTAL_NUMBER_OF_DEPLOYMENTS, totalDeploymentCount);
    telemetryReporter.sendGroupEvent(
        accountId, null, map, null, TelemetryOption.builder().sendForCommunity(true).build());
  }

  private String getAccountId() {
    return "";
  }
}
