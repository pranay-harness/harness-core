package io.harness.notifications.conditions;

import io.harness.notifications.FilterMatcher;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

@Value
public class CVFilterMatcher implements FilterMatcher {
  private static final Logger log = LoggerFactory.getLogger(ManualInterventionFilterMatcher.class);

  private AlertFilter alertFilter;
  private Alert alert;

  @Override
  public boolean matchesCondition() {
    Conditions filterConditions = alertFilter.getConditions();
    Operator op = filterConditions.getOperator();
    CVFilters cvAlertFilters = filterConditions.getCvAlertFilters();

    if (null == cvAlertFilters) {
      log.info("No cvAlertFilters specified. Alert will be considered to match filter.");
      return true;
    }

    ContinuousVerificationAlertData alertData = (ContinuousVerificationAlertData) alert.getAlertData();
    if (null == alertData) {
      log.error("CV Alert data is null. Alert: {}", alert);
      return false;
    }

    List<Supplier<Boolean>> conditions = new LinkedList<>();
    conditions.add(() -> alert.getType() == alertFilter.getAlertType());

    List<String> appIds = cvAlertFilters.getAppIds();
    conditions.add(() -> appIds.contains(alert.getAppId()));

    List<String> envIds = cvAlertFilters.getEnvIds();
    conditions.add(() -> envIds.contains(alertData.getCvConfiguration().getEnvId()));

    List<String> cvConfigIds = cvAlertFilters.getCVConfigurationIds();
    conditions.add(() -> cvConfigIds.contains(alertData.getCvConfiguration().getUuid()));

    boolean matches = allTrue(conditions);

    switch (op) {
      case MATCHING:
        return matches;
      case NOT_MATCHING:
        return !matches;
      default:
        throw new IllegalArgumentException("Unexpected value of alert filter operator: " + op);
    }
  }

  private static boolean allTrue(List<Supplier<Boolean>> booleanFns) {
    for (Supplier<Boolean> fn : booleanFns) {
      if (!fn.get()) {
        return false;
      }
    }

    return true;
  }
}
