package software.wings.service.impl.instance.licensing;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.InstanceUsageLimitAlert;
import software.wings.service.impl.AlertServiceImpl;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;

import java.util.Optional;

public class InstanceUsageLimitExcessHandlerImplTest extends WingsBaseTest {
  @Mock private InstanceUsageLimitChecker limitChecker;
  @Mock private AlertServiceImpl alertService;

  @InjectMocks @Inject private InstanceUsageLimitExcessHandler usageLimitExcessHandler;

  @Test
  @Category(UnitTests.class)
  public void testAlertRaised() {
    String accountId = "some-account-id";
    long percentLimit = 90L;

    when(limitChecker.isWithinLimit(accountId, percentLimit, 10D)).thenReturn(false);
    when(alertService.findExistingAlert(eq(accountId), eq(Base.GLOBAL_APP_ID),
             eq(AlertType.INSTANCE_USAGE_APPROACHING_LIMIT), any(InstanceUsageLimitAlert.class)))
        .thenReturn(Optional.empty());

    usageLimitExcessHandler.handle(accountId, 10D);

    Optional<Alert> alert = Optional.of(sampleAlert(accountId));

    // after first time, alert should be saved
    when(alertService.findExistingAlert(eq(accountId), eq(Base.GLOBAL_APP_ID),
             eq(AlertType.INSTANCE_USAGE_APPROACHING_LIMIT), any(InstanceUsageLimitAlert.class)))
        .thenReturn(alert);

    usageLimitExcessHandler.handle(accountId, 10D);
    verify(alertService, times(1))
        .openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT,
            InstanceUsageLimitExcessHandlerImpl.createAlertData(accountId, percentLimit));
  }

  @Test
  @Category(UnitTests.class)
  public void testAlertNotRaisedIfExisting() {
    String accountId = "some-account-id";
    long percentLimit = 90L;

    when(limitChecker.isWithinLimit(accountId, percentLimit, 10D)).thenReturn(false);

    when(alertService.findExistingAlert(eq(accountId), eq(Base.GLOBAL_APP_ID),
             eq(AlertType.INSTANCE_USAGE_APPROACHING_LIMIT), any(InstanceUsageLimitAlert.class)))
        .thenReturn(Optional.of(sampleAlert(accountId)));

    usageLimitExcessHandler.handle(accountId, 10D);
    verify(alertService, times(0))
        .openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT,
            InstanceUsageLimitExcessHandlerImpl.createAlertData(accountId, percentLimit));
  }

  @Test
  @Category(UnitTests.class)
  public void testAlertIsClosedWhenWithinLimit() {
    String accountId = "some-account-id";
    long percentLimit = 90L;

    when(limitChecker.isWithinLimit(accountId, percentLimit, 10D)).thenReturn(true);
    usageLimitExcessHandler.handle(accountId, 10D);
    verify(alertService)
        .closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT,
            InstanceUsageLimitExcessHandlerImpl.createAlertData(accountId, percentLimit));
  }

  private static Alert sampleAlert(String accountId) {
    Alert alert = new Alert();
    alert.setAccountId(accountId);

    return alert;
  }
}