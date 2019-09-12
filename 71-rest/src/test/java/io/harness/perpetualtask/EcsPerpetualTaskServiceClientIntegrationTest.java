package io.harness.perpetualtask;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskClientParams;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import lombok.val;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.BaseIntegrationTest;

public class EcsPerpetualTaskServiceClientIntegrationTest extends BaseIntegrationTest {
  @Inject private EcsPerpetualTaskServiceClient ecsPerpetualTaskServiceClient;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;

  private final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String DEFAULT_REGION = "region";
  private final String DEFAULT_SETTING_ID = "settingId";
  private final String DEFAULT_CLUSTER_NAME = "clusterName";

  @Test
  @Category(IntegrationTests.class)
  public void shouldCreatePerpetualTask() {
    EcsPerpetualTaskClientParams ecsPerpetualTaskClientParams =
        new EcsPerpetualTaskClientParams(DEFAULT_REGION, DEFAULT_SETTING_ID, DEFAULT_CLUSTER_NAME);
    String taskId = ecsPerpetualTaskServiceClient.create(TEST_ACCOUNT_ID, ecsPerpetualTaskClientParams);
    assertThat(taskId).isNotNull();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecordDao.getTask(taskId);
    assertThat(perpetualTaskRecord).isNotNull();
    assertThat(perpetualTaskRecord.getClientContext().getClientParams().get("region")).isEqualTo(DEFAULT_REGION);
  }

  @After
  public void clearCollection() {
    val ds = wingsPersistence.getDatastore(PerpetualTaskRecord.class);
    ds.delete(ds.createQuery(PerpetualTaskRecord.class).filter(PerpetualTaskRecordKeys.accountId, TEST_ACCOUNT_ID));
  }
}
