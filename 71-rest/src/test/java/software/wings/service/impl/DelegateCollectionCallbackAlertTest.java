package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static io.harness.threading.Morpheus.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.rule.OwnerRule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertStatus;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.cv.ContinuousVerificationDataCollectionAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DelegateCollectionCallbackAlertTest extends WingsBaseTest {
  private String appId;
  private String accountId;
  private String cvConfigId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private AlertService alertService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private CVActivityLogService cvActivityLogService;
  @Mock private WaitNotifyEngine waitNotifyEngine;

  @Before
  public void setUp() {
    accountId = wingsPersistence.save(anAccount().withAccountName(generateUuid()).build());
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    CVConfiguration cvConfiguration = new CVConfiguration();
    cvConfiguration.setName(generateUuid());
    cvConfiguration.setAppId(appId);
    cvConfigId = wingsPersistence.save(cvConfiguration);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testCVDataCollectionAlert() {
    DataCollectionCallback dataCollectionCallback =
        DataCollectionCallback.builder().appId(appId).cvConfigId(cvConfigId).build();
    dataCollectionCallback.setAlertService(alertService);
    dataCollectionCallback.setAppService(appService);
    dataCollectionCallback.setWaitNotifyEngine(waitNotifyEngine);
    dataCollectionCallback.setCvConfigurationService(cvConfigurationService);
    dataCollectionCallback.setCvActivityLogService(cvActivityLogService);

    dataCollectionCallback.setExecutionData(new VerificationStateAnalysisExecutionData());

    Map<String, ResponseData> response = new HashMap<>();
    response.put("error", ErrorNotifyResponseData.builder().errorMessage("some error message").build());

    dataCollectionCallback.notifyError(response);
    sleep(Duration.ofMillis(2000));
    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter("accountId", Operator.EQ, accountId).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT);
    assertThat(alert.getCategory()).isEqualTo(AlertCategory.ContinuousVerification);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Open);
    assertThat(((ContinuousVerificationDataCollectionAlert) alert.getAlertData()).getCvConfiguration().getUuid())
        .isEqualTo(cvConfigId);

    response.put("error", DataCollectionTaskResult.builder().status(DataCollectionTaskStatus.SUCCESS).build());
    dataCollectionCallback.notify(response);
    sleep(Duration.ofMillis(2000));
    alerts = alertService.list(aPageRequest().addFilter("accountId", Operator.EQ, accountId).build());
    assertThat(alerts).hasSize(1);
    alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT);
    assertThat(alert.getCategory()).isEqualTo(AlertCategory.ContinuousVerification);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Closed);
    assertThat(((ContinuousVerificationDataCollectionAlert) alert.getAlertData()).getCvConfiguration().getUuid())
        .isEqualTo(cvConfigId);

    // reopen alert
    response.put("error", ErrorNotifyResponseData.builder().errorMessage("some error message").build());

    dataCollectionCallback.notifyError(response);
    sleep(Duration.ofMillis(2000));
    alerts = alertService.list(aPageRequest()
                                   .addFilter("accountId", Operator.EQ, accountId)
                                   .addFilter("status", Operator.EQ, AlertStatus.Open)
                                   .build());
    assertThat(alerts).hasSize(1);
    alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT);
    assertThat(alert.getCategory()).isEqualTo(AlertCategory.ContinuousVerification);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Open);
    assertThat(((ContinuousVerificationDataCollectionAlert) alert.getAlertData()).getCvConfiguration().getUuid())
        .isEqualTo(cvConfigId);
  }
}
