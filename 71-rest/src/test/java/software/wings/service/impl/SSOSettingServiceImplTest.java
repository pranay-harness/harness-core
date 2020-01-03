package software.wings.service.impl;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.service.intfc.AlertService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Deepak Patankar
 * 21/Oct/2019
 */
public class SSOSettingServiceImplTest extends WingsBaseTest {
  @Mock private AlertService alertService;
  @Inject @InjectMocks private SSOSettingServiceImpl ssoSettingService;
  private String accountId = "accountId";
  private String ssoId = "ssoID";
  private String message = "errorMessage";

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testRaiseSyncFailureAlert() {
    doAnswer(i -> CompletableFuture.completedFuture("0")).when(alertService).openAlert(any(), any(), any(), any());

    // case when a alert exists and is created within 24 hours, we should not create a alert
    Alert newAlert =
        Alert.builder().uuid("alertId").appId("applicationId").lastUpdatedAt(System.currentTimeMillis()).build();
    when(alertService.findExistingAlert(
             any(String.class), any(String.class), any(AlertType.class), any(AlertData.class)))
        .thenReturn(Optional.of(newAlert));
    ssoSettingService.raiseSyncFailureAlert(accountId, ssoId, message);
    verify(alertService, times(0)).openAlert(any(), any(), any(), any());

    // case when a alert exists and before 24 hours, we need to create a alert and send mail
    long oldTime = System.currentTimeMillis() - 86400001;
    Alert oldAlert = Alert.builder().uuid("alertId").appId("applicationId").lastUpdatedAt(oldTime).build();
    when(alertService.findExistingAlert(
             any(String.class), any(String.class), any(AlertType.class), any(AlertData.class)))
        .thenReturn(Optional.of(oldAlert));
    ssoSettingService.raiseSyncFailureAlert(accountId, ssoId, message);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    // Case when no such alert exist, so we will create a new alert
    when(alertService.findExistingAlert(
             any(String.class), any(String.class), any(AlertType.class), any(AlertData.class)))
        .thenReturn(Optional.empty());
    ssoSettingService.raiseSyncFailureAlert(accountId, ssoId, message);
    verify(alertService, times(2)).openAlert(any(), any(), any(), any());
  }
}