package io.harness.notifications.conditions;

import io.harness.notifications.FilterMatcher;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import io.harness.notifications.beans.ManualInterventionAlertFilters;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.ManualInterventionNeededAlert;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

@Value
public class ManualInterventionFilterMatcher implements FilterMatcher {
  private static final Logger log = LoggerFactory.getLogger(ManualInterventionFilterMatcher.class);

  private AlertFilter alertFilter;
  private Alert alert;

  @Override
  public boolean matchesCondition() {
    Conditions filterConditions = alertFilter.getConditions();
    Operator op = filterConditions.getOperator();

    ManualInterventionAlertFilters manualInterventionAlertFilters = filterConditions.getManualInterventionFilters();
    if (null == manualInterventionAlertFilters) {
      log.error("No manualInterventionFilters specified. Alert will be considered to not match filter.");
      return false;
    }

    List<Supplier<Boolean>> conditions = new LinkedList<>();
    conditions.add(() -> alert.getType() == alertFilter.getAlertType());

    List<String> appIds = manualInterventionAlertFilters.getAppIds();
    conditions.add(() -> appIds.contains(alert.getAppId()));

    List<String> envIds = manualInterventionAlertFilters.getEnvIds();

    conditions.add(() -> {
      ManualInterventionNeededAlert alertData = (ManualInterventionNeededAlert) alert.getAlertData();
      if (null == alertData) {
        log.error("Manual Intervention Alert data is null. Alert: {}", alert);
        return false;
      }
      return envIds.contains(alertData.getEnvId());
    });

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

  private boolean allTrue(List<Supplier<Boolean>> booleanFns) {
    for (Supplier<Boolean> fn : booleanFns) {
      if (!fn.get()) {
        return false;
      }
    }

    return true;
  }
}
