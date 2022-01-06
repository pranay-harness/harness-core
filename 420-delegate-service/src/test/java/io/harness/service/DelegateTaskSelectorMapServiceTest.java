/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.exception.NoResultFoundException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateTaskSelectorMapService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class DelegateTaskSelectorMapServiceTest extends DelegateServiceTestBase {
  private static final String ACCOUNT_ID = generateUuid();
  private static final TaskGroup HELM_TASK_GROUP = TaskGroup.HELM;
  @Inject private HPersistence hPersistence;
  @InjectMocks @Inject private DelegateTaskSelectorMapService taskSelectorMapService;

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldList() {
    TaskSelectorMap taskSelectorMap = TaskSelectorMap.builder().accountId(ACCOUNT_ID).build();
    hPersistence.save(taskSelectorMap);
    TaskSelectorMap taskSelectorMap2 = TaskSelectorMap.builder().accountId(ACCOUNT_ID + "2").build();
    hPersistence.save(taskSelectorMap2);
    assertThat(taskSelectorMapService.list(ACCOUNT_ID)).hasSize(1).containsExactly(taskSelectorMap);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAdd() {
    TaskSelectorMap taskSelectorMapToAdd = TaskSelectorMap.builder()
                                               .accountId(ACCOUNT_ID)
                                               .taskGroup(HELM_TASK_GROUP)
                                               .selectors(new HashSet(asList("a", "b")))
                                               .build();
    TaskSelectorMap newTaskSelector = taskSelectorMapService.add(taskSelectorMapToAdd);
    assertThat(newTaskSelector.getUuid()).isNotNull();
    assertThat(newTaskSelector).isEqualToIgnoringGivenFields(taskSelectorMapToAdd, "uuid");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldUpdate() {
    TaskSelectorMap taskSelectorMap =
        TaskSelectorMap.builder().taskGroup(HELM_TASK_GROUP).accountId(ACCOUNT_ID).selectors(singleton("a")).build();
    hPersistence.save(taskSelectorMap);
    TaskSelectorMap taskSelectorMapToUpdate = TaskSelectorMap.builder()
                                                  .uuid(taskSelectorMap.getUuid())
                                                  .accountId(ACCOUNT_ID)
                                                  .taskGroup(HELM_TASK_GROUP)
                                                  .selectors(new HashSet(asList("a", "b", "c")))
                                                  .build();
    taskSelectorMapService.update(taskSelectorMapToUpdate);
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    taskSelectorMap.setSelectors(taskSelectorMapToUpdate.getSelectors());
    assertThat(updated).isEqualToIgnoringGivenFields(taskSelectorMap, "lastUpdatedAt");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldUpdateIfEmptySelectorsDelete() {
    TaskSelectorMap taskSelectorMap =
        TaskSelectorMap.builder().taskGroup(HELM_TASK_GROUP).accountId(ACCOUNT_ID).selectors(singleton("a")).build();
    hPersistence.save(taskSelectorMap);
    TaskSelectorMap taskSelectorMapToUpdate = TaskSelectorMap.builder()
                                                  .uuid(taskSelectorMap.getUuid())
                                                  .accountId(ACCOUNT_ID)
                                                  .taskGroup(HELM_TASK_GROUP)
                                                  .selectors(null)
                                                  .build();
    taskSelectorMapService.update(taskSelectorMapToUpdate);
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(updated).isNull();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAddSelector() {
    TaskSelectorMap taskSelectorMap =
        TaskSelectorMap.builder().taskGroup(HELM_TASK_GROUP).accountId(ACCOUNT_ID).selectors(singleton("a")).build();
    hPersistence.save(taskSelectorMap);
    taskSelectorMapService.addTaskSelector(ACCOUNT_ID, taskSelectorMap.getUuid(), "b");
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(updated).isNotNull();
    assertThat(updated.getUuid()).isEqualTo(taskSelectorMap.getUuid());
    assertThat(updated.getSelectors()).containsExactly("a", "b");
  }

  @Test(expected = NoResultFoundException.class)
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAddTaskSelectorHandleEmpty() {
    taskSelectorMapService.addTaskSelector(ACCOUNT_ID, "13", "b");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAddSelectorIgnore() {
    TaskSelectorMap taskSelectorMap =
        TaskSelectorMap.builder().taskGroup(HELM_TASK_GROUP).accountId(ACCOUNT_ID).selectors(singleton("a")).build();
    hPersistence.save(taskSelectorMap);
    taskSelectorMapService.addTaskSelector(ACCOUNT_ID, taskSelectorMap.getUuid(), "a");
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(updated).isNotNull();
    assertThat(updated.getUuid()).isEqualTo(taskSelectorMap.getUuid());
    assertThat(updated.getSelectors()).containsExactly("a");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRemoveSelector() {
    TaskSelectorMap taskSelectorMap = TaskSelectorMap.builder()
                                          .taskGroup(HELM_TASK_GROUP)
                                          .accountId(ACCOUNT_ID)
                                          .selectors(new HashSet(asList("a", "b")))
                                          .build();
    hPersistence.save(taskSelectorMap);
    taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, taskSelectorMap.getUuid(), "b");
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(updated).isNotNull();
    assertThat(updated.getUuid()).isEqualTo(taskSelectorMap.getUuid());
    assertThat(updated.getSelectors()).containsExactly("a");
  }

  @Test(expected = NoResultFoundException.class)
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRemoveTaskSelectorHandleEmpty() {
    taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, "13", "b");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRemoveSelectorIgnore() {
    TaskSelectorMap taskSelectorMap = TaskSelectorMap.builder()
                                          .taskGroup(HELM_TASK_GROUP)
                                          .accountId(ACCOUNT_ID)
                                          .selectors(new HashSet(asList("a", "b")))
                                          .build();
    hPersistence.save(taskSelectorMap);
    taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, taskSelectorMap.getUuid(), "c");
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(updated).isNotNull();
    assertThat(updated.getUuid()).isEqualTo(taskSelectorMap.getUuid());
    assertThat(updated.getSelectors()).containsExactly("a", "b");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRemoveSelectorMapCompletely() {
    TaskSelectorMap taskSelectorMap =
        TaskSelectorMap.builder().taskGroup(HELM_TASK_GROUP).accountId(ACCOUNT_ID).selectors(singleton("a")).build();
    hPersistence.save(taskSelectorMap);
    taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, taskSelectorMap.getUuid(), "a");
    TaskSelectorMap updated = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(updated).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldGetTaskSelectorMap() {
    String accountId = generateUuid();
    TaskGroup taskGroup = TaskGroup.HTTP;

    Set<String> commandMapSelectors = new HashSet<>();
    commandMapSelectors.add("e");
    TaskSelectorMap taskSelectorMap = TaskSelectorMap.builder()
                                          .accountId(ACCOUNT_ID)
                                          .taskGroup(TaskGroup.HTTP)
                                          .selectors(commandMapSelectors)
                                          .build();
    hPersistence.save(taskSelectorMap);

    taskSelectorMapService.get(accountId, taskGroup);

    TaskSelectorMap fetchTaskSelectorMap = hPersistence.get(TaskSelectorMap.class, taskSelectorMap.getUuid());
    assertThat(fetchTaskSelectorMap).isNotNull();
    assertThat(fetchTaskSelectorMap.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(fetchTaskSelectorMap.getTaskGroup()).isEqualTo(TaskGroup.HTTP);
    assertThat(fetchTaskSelectorMap.getSelectors()).containsExactly("e");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotGetTaskSelectorMap() {
    TaskSelectorMap getTaskSelectorMap = taskSelectorMapService.get(ACCOUNT_ID, TaskGroup.HTTP);
    assertThat(getTaskSelectorMap).isNull();
  }
}
