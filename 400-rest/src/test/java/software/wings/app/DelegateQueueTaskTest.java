package software.wings.app;

import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateQueueTaskTest extends WingsBaseTest {
  @Inject DelegateQueueTask delegateQueueTask;
  @Inject HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEndTasksWithCorruptedRecord() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId("FOO")
                                    .expiry(System.currentTimeMillis() - 10)
                                    .data(TaskData.builder().timeout(1).build())
                                    .build();
    persistence.save(delegateTask);
    persistence.update(delegateTask,
        persistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.data_parameters, "dummy".toCharArray()));

    delegateQueueTask.endTasks(asList(delegateTask.getUuid()));

    assertThat(persistence.createQuery(DelegateTask.class).count()).isEqualTo(0);
  }
}
