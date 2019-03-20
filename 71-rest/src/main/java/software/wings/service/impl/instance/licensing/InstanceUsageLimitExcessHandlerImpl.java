package software.wings.service.impl.instance.licensing;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.InstanceUsageLimitAlert;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;

import java.util.Optional;

public class InstanceUsageLimitExcessHandlerImpl implements InstanceUsageLimitExcessHandler {
  private static final Logger log = LoggerFactory.getLogger(InstanceUsageLimitExcessHandlerImpl.class);

  private InstanceUsageLimitChecker limitChecker;
  private AlertService alertService;

  @Inject
  public InstanceUsageLimitExcessHandlerImpl(InstanceUsageLimitChecker limitChecker, AlertService alertService) {
    this.limitChecker = limitChecker;
    this.alertService = alertService;
  }

  private static final String WARNING_MESSAGE =
      "{}% of allowed instance limits has been consumed. Please contact Harness support to increase limits.";

  @Override
  public void handle(String accountId, double actualUsage) {
    long percentLimit = 90L;
    boolean withinLimit = limitChecker.isWithinLimit(accountId, percentLimit, actualUsage);
    AlertData alertData = createAlertData(accountId, percentLimit);

    if (!withinLimit) {
      Optional<Alert> alert = alertService.findExistingAlert(
          accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT, alertData);
      if (!alert.isPresent()) {
        alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT, alertData);
      } else {
        log.info("Alert already exists. Skipping creation. Alert Data: {}", alertData);
      }
    } else {
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT, alertData);
    }
  }

  @VisibleForTesting
  static InstanceUsageLimitAlert createAlertData(String accountId, long percentLimit) {
    String warningMsg = MessageFormatter.format(WARNING_MESSAGE, percentLimit).getMessage();
    return new InstanceUsageLimitAlert(accountId, percentLimit, warningMsg);
  }
}