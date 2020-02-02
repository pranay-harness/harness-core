package io.harness.perpetualtask.internal;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.BaseIntegrationTest;

@Slf4j
public class PerpetualTaskRecordDaoIntegrationTest extends BaseIntegrationTest {
  @Inject PerpetualTaskRecordDao taskRecordDao;

  private final String accountId = "test-account-id";
  private final String delegateId1 = "test-delegate-id1";
  private final String delegateId2 = "test-delegate-id2";
  private final PerpetualTaskRecord taskRecord1 =
      PerpetualTaskRecord.builder().accountId(accountId).delegateId(delegateId1).build();
  private final PerpetualTaskRecord taskRecord2 =
      PerpetualTaskRecord.builder().accountId(accountId).delegateId(delegateId2).build();
  String taskId1;
  String taskId2;

  @Test
  @Owner(developers = HANTANG)
  @Category(DeprecatedIntegrationTests.class)
  public void testResetDelegateId() {
    // insert two perpetual task records
    taskId1 = taskRecordDao.save(taskRecord1);
    taskId2 = taskRecordDao.save(taskRecord2);
    taskRecordDao.resetDelegateId(accountId, delegateId1);
    // assert that the task with taskId=taskId1 has been reset
    assertThat(taskRecordDao.getTask(taskId1).getDelegateId()).isEqualTo("");
    // assert that the task with taskId=taskId2 remain the same
    assertThat(taskRecordDao.getTask(taskId2).getDelegateId()).isEqualTo(delegateId2);
  }

  @After
  public void clearData() {
    logger.info("Cleaning up data..");
    taskRecordDao.remove(accountId, taskId1);
    taskRecordDao.remove(accountId, taskId2);
  }
}
