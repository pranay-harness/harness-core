package software.wings.service.impl.instance.licensing;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.alert.AlertData;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.counter.service.CounterService;
import org.slf4j.helpers.MessageFormatter;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.InstanceUsageLimitAlert;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;

public class InstanceUsageLimitExcessHandlerImpl implements InstanceUsageLimitExcessHandler {
  private InstanceUsageLimitChecker limitChecker;
  private AlertService alertService;
  private CounterService counterService;

  @Inject
  public InstanceUsageLimitExcessHandlerImpl(
      InstanceUsageLimitChecker limitChecker, AlertService alertService, CounterService counterService) {
    this.limitChecker = limitChecker;
    this.alertService = alertService;
    this.counterService = counterService;
  }

  private static final String WARNING_MESSAGE =
      "{}% of allowed instance limits has been consumed. Please contact Harness support to increase limits.";

  @Override
  public void handle(String accountId, double actualUsage) {
    long percentLimit = 90L;
    boolean withinLimit = limitChecker.isWithinLimit(accountId, percentLimit, actualUsage);
    AlertData alertData = createAlertData(accountId, percentLimit);

    if (!withinLimit) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT, alertData);
    } else {
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT, alertData);
    }
  }

  @Override
  public void updateViolationCount(String accountId, double actualUsage) {
    boolean withinLimit = limitChecker.isWithinLimit(accountId, 100L, actualUsage);
    Action action = new Action(accountId, ActionType.INSTANCE_USAGE_LIMIT_EXCEEDED);

    if (!withinLimit) {
      int valueOnInsert = 1;
      counterService.increment(action, valueOnInsert);
    } else {
      // reset counter
      counterService.upsert(new Counter(action.key(), 0));
    }
  }

  @VisibleForTesting
  static InstanceUsageLimitAlert createAlertData(String accountId, long percentLimit) {
    String warningMsg = MessageFormatter.format(WARNING_MESSAGE, percentLimit).getMessage();
    return new InstanceUsageLimitAlert(accountId, percentLimit, warningMsg);
  }
}
