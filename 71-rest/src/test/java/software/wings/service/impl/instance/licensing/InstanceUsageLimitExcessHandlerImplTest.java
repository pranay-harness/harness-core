package software.wings.service.impl.instance.licensing;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.alert.AlertType;
import software.wings.service.impl.AlertServiceImpl;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitChecker;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;

public class InstanceUsageLimitExcessHandlerImplTest extends WingsBaseTest {
  @Mock private InstanceUsageLimitChecker limitChecker;
  @Mock private AlertServiceImpl alertService;

  @InjectMocks @Inject private InstanceUsageLimitExcessHandler usageLimitExcessHandler;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAlertRaised() {
    String accountId = "some-account-id";
    long percentLimit = 90L;

    when(limitChecker.isWithinLimit(accountId, percentLimit, 10D)).thenReturn(false);
    usageLimitExcessHandler.handle(accountId, 10D);
    verify(alertService)
        .openAlert(accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT,
            InstanceUsageLimitExcessHandlerImpl.createAlertData(accountId, percentLimit));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAlertIsClosedWhenWithinLimit() {
    String accountId = "some-account-id";
    long percentLimit = 90L;

    when(limitChecker.isWithinLimit(accountId, percentLimit, 10D)).thenReturn(true);
    usageLimitExcessHandler.handle(accountId, 10D);
    verify(alertService)
        .closeAlert(accountId, GLOBAL_APP_ID, AlertType.INSTANCE_USAGE_APPROACHING_LIMIT,
            InstanceUsageLimitExcessHandlerImpl.createAlertData(accountId, percentLimit));
  }
}
