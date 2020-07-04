package io.harness.beans;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FactoryMethodTests extends CategoryTest {
  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void asyncTaskDataTest() {
    String taskType = "taskType";
    Object[] parameters = new Object[] {new Object(), new Object()};
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(taskType)
                            .parameters(parameters)
                            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                            .build();
    assertThat(taskData).isEqualTo(TaskData.asyncTaskData(taskType, parameters));
  }
}
