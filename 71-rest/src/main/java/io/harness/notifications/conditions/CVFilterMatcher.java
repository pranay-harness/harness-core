package io.harness.notifications.conditions;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.notifications.FilterMatcher;
import io.harness.notifications.beans.CVAlertFilters;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

@Value
@Slf4j
public class CVFilterMatcher implements FilterMatcher {
  private AlertFilter alertFilter;
  private Alert alert;

  @Override
  public boolean matchesCondition() {
    Conditions filterConditions = alertFilter.getConditions();
    Operator op = filterConditions.getOperator();
    CVAlertFilters cvAlertFilters = filterConditions.getCvAlertFilters();

    if (null == cvAlertFilters) {
      logger.info("No cvAlertFilters specified. Alert will be considered to match filter.");
      return true;
    }

    ContinuousVerificationAlertData alertData = (ContinuousVerificationAlertData) alert.getAlertData();
    if (null == alertData) {
      logger.error("CV Alert data is null. Alert: {}", alert);
      return false;
    }

    List<Supplier<Boolean>> conditions = new LinkedList<>();
    conditions.add(() -> alert.getType() == alertFilter.getAlertType());

    List<String> appIds = cvAlertFilters.getAppIds();
    if (isNotEmpty(appIds)) {
      conditions.add(() -> appIds.contains(alert.getAppId()));
    }

    List<String> envIds = cvAlertFilters.getEnvIds();
    if (isNotEmpty(envIds)) {
      conditions.add(() -> envIds.contains(alertData.getCvConfiguration().getEnvId()));
    }

    List<String> cvConfigIds = cvAlertFilters.getCvConfigIds();
    if (isNotEmpty(cvConfigIds)) {
      conditions.add(() -> cvConfigIds.contains(alertData.getCvConfiguration().getUuid()));
    }

    if (cvAlertFilters.getAlertMinThreshold() >= 0) {
      conditions.add(() -> alertData.getRiskScore() >= cvAlertFilters.getAlertMinThreshold());
    }

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
