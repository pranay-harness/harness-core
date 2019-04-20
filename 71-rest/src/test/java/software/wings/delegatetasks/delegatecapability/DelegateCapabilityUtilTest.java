package software.wings.delegatetasks.delegatecapability;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.CapabilityUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.TaskType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DelegateCapabilityUtilTest {
  private static Set<String> taskGroupsMoved =
      new HashSet<>(Arrays.asList("HTTP", "JENKINS", "BAMBOO", "GCS", "DOCKER", "NEXUS", "ARTIFACTORY"));

  @Test
  @Category(UnitTests.class)
  public void testAllExpectedTaskMovedToCapabilityFramework() {
    List<TaskType> taskTypeList =
        Arrays.stream(TaskType.values())
            .filter(taskType -> isTaskGroupMovedToCapabilityFramework(taskType.getTaskGroup().name()))
            .collect(toList());

    assertNotNull(taskTypeList);

    List<TaskType> taskTypesVerified =
        taskTypeList.stream()
            .filter(taskType -> CapabilityUtils.isTaskTypeMigratedToCapabilityFramework(taskType.name()))
            .collect(toList());

    assertEquals(taskTypeList.size(), taskTypesVerified.size());
  }

  private boolean isTaskGroupMovedToCapabilityFramework(String taskGroupName) {
    return taskGroupsMoved.contains(taskGroupName);
  }
}
