package software.wings.delegatetasks.delegatecapability;

import static io.harness.rule.OwnerRule.ADWAIT;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.CapabilityUtils;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.TaskType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DelegateCapabilityUtilsTest extends CategoryTest {
  private static Set<String> taskGroupsMoved =
      ImmutableSet.of("JENKINS", "BAMBOO", "GCS", "DOCKER", "NEXUS", "ARTIFACTORY");

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAllExpectedTaskMovedToCapabilityFramework() {
    List<TaskType> taskTypeList =
        Arrays.stream(TaskType.values())
            .filter(taskType -> isTaskGroupMovedToCapabilityFramework(taskType.getTaskGroup().name()))
            .collect(toList());

    assertThat(taskTypeList).isNotNull();

    List<TaskType> taskTypesVerified =
        taskTypeList.stream()
            .filter(taskType -> CapabilityUtils.isTaskTypeMigratedToCapabilityFramework(taskType.name()))
            .collect(toList());

    assertThat(taskTypesVerified).hasSize(taskTypeList.size());
  }

  private boolean isTaskGroupMovedToCapabilityFramework(String taskGroupName) {
    return taskGroupsMoved.contains(taskGroupName);
  }
}
