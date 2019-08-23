package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.verification.CVTaskService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CVTaskServiceTest extends BaseIntegrationTest {
  @Inject CVTaskService cvTaskService;
  @Before
  public void setupTests() {}
  @Test
  @Category(IntegrationTests.class)
  public void testEnqueueTask() {
    String cvConfigId = generateUUID();
    long endMS = System.currentTimeMillis();
    long startMS = endMS - TimeUnit.MINUTES.toMillis(10);
    CVTask cvTask = cvTaskService.enqueueTask(accountId, cvConfigId, startMS, endMS);
    assertThat(cvTask.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLastCVTask() {
    String cvConfigId = generateUUID();
    long now = System.currentTimeMillis();
    long startMS1 = now - TimeUnit.MINUTES.toMillis(30);
    long endMS1 = now - TimeUnit.MINUTES.toMillis(15);

    assertThat(Optional.empty()).isEqualTo(cvTaskService.getLastCVTask(accountId, cvConfigId));

    CVTask cvTask1 = cvTaskService.enqueueTask(accountId, cvConfigId, startMS1, endMS1);
    Optional<CVTask> lastCvTask = cvTaskService.getLastCVTask(accountId, cvConfigId);
    assertThat(lastCvTask.get().getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(lastCvTask.get().getEndMilliSec()).isEqualTo(endMS1);
    assertThat(lastCvTask.get().getStartMilliSec()).isEqualTo(startMS1);

    long startMS2 = now - TimeUnit.MINUTES.toMillis(10);
    long endMS2 = now - TimeUnit.MINUTES.toMillis(5);

    cvTaskService.enqueueTask(accountId, cvConfigId, startMS2, endMS2);
    lastCvTask = cvTaskService.getLastCVTask(accountId, cvConfigId);
    assertThat(lastCvTask.get().getCvConfigId()).isEqualTo(cvTask1.getCvConfigId());
    assertThat(lastCvTask.get().getEndMilliSec()).isEqualTo(endMS2);
    assertThat(lastCvTask.get().getStartMilliSec()).isEqualTo(startMS2);
  }
}
