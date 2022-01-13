/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SANJA;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.TAG_NAME;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.eraro.ErrorCode;
import io.harness.exception.NoResultFoundException;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateTaskSelectorMapService;

import software.wings.exception.WingsExceptionMapper;
import software.wings.utils.ResourceTestRule;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpStatus;

public class DelegateTaskSelectorMapResourceTest extends CategoryTest {
  private static DelegateTaskSelectorMapService taskSelectorMapService = mock(DelegateTaskSelectorMapService.class);
  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

  @Parameterized.Parameter public String apiUrl;

  @Parameterized.Parameters
  public static String[] data() {
    return new String[] {null, "https://testUrl"};
  }

  @Rule public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static final ResourceTestRule RESOURCES =

      ResourceTestRule.builder()
          .instance(new DelegateTaskSelectorMapResource(taskSelectorMapService))
          .instance(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .type(WingsExceptionMapper.class)
          .build();

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldListTaskSelectorMaps() {
    PageResponse<TaskSelectorMap> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(TaskSelectorMap.builder()
                                        .accountId(ACCOUNT_ID)
                                        .selectors(singleton(TAG_NAME))
                                        .taskGroup(TaskGroup.HELM)
                                        .build()));
    pageResponse.setTotal(1l);
    when(taskSelectorMapService.list(ACCOUNT_ID)).thenReturn(pageResponse);
    RestResponse<PageResponse<TaskSelectorMap>> restResponse =
        RESOURCES.client()
            .target("/delegate-task-selector-map?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<TaskSelectorMap>>>() {});
    PageRequest<TaskSelectorMap> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    verify(taskSelectorMapService, atLeastOnce()).list(ACCOUNT_ID);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAddTaskSelectorMap() {
    TaskSelectorMap newTaskSelector =
        TaskSelectorMap.builder().accountId(ACCOUNT_ID).selectors(singleton(TAG_NAME)).build();
    when(taskSelectorMapService.add(newTaskSelector)).thenReturn(newTaskSelector);
    RestResponse<TaskSelectorMap> restResponse = RESOURCES.client()
                                                     .target("/delegate-task-selector-map?accountId=" + ACCOUNT_ID)
                                                     .request()
                                                     .post(entity(newTaskSelector, MediaType.APPLICATION_JSON),
                                                         new GenericType<RestResponse<TaskSelectorMap>>() {});

    verify(taskSelectorMapService, atLeastOnce()).add(newTaskSelector);
    assertThat(restResponse.getResource()).isEqualTo(newTaskSelector);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldUpdateTaskSelectorMap() {
    TaskSelectorMap newTaskSelector =
        TaskSelectorMap.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).selectors(singleton(TAG_NAME)).build();
    when(taskSelectorMapService.update(newTaskSelector)).thenReturn(newTaskSelector);
    RestResponse<TaskSelectorMap> restResponse =
        RESOURCES.client()
            .target(String.format("/delegate-task-selector-map/%s?accountId=%s", newTaskSelector.getUuid(), ACCOUNT_ID))
            .request()
            .put(entity(newTaskSelector, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<TaskSelectorMap>>() {});

    verify(taskSelectorMapService, atLeastOnce()).update(newTaskSelector);
    assertThat(restResponse.getResource()).isEqualTo(newTaskSelector);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAddTaskSelector() {
    String taskSelectorMapUuid = generateUuid();
    TaskSelectorMap newTaskSelector =
        TaskSelectorMap.builder().uuid(taskSelectorMapUuid).accountId(ACCOUNT_ID).selectors(singleton("new")).build();
    when(taskSelectorMapService.addTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME)).thenReturn(newTaskSelector);
    RestResponse<TaskSelectorMap> restResponse =
        RESOURCES.client()
            .target(String.format("/delegate-task-selector-map/%s/task-selectors?accountId=%s&selector=%s",
                taskSelectorMapUuid, ACCOUNT_ID, TAG_NAME))
            .request()
            .post(null, new GenericType<RestResponse<TaskSelectorMap>>() {});
    verify(taskSelectorMapService, atLeastOnce()).addTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME);
    assertThat(restResponse.getResource()).isEqualTo(newTaskSelector);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldAddTaskSelectorMapThrowNotFound() {
    String taskSelectorMapUuid = generateUuid();
    thrown.expect(Matchers.hasProperty(
        "response", Matchers.hasProperty("status", CoreMatchers.is(HttpStatus.NOT_FOUND.value()))));

    when(taskSelectorMapService.addTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME))
        .thenThrow(NoResultFoundException.newBuilder().code(ErrorCode.RESOURCE_NOT_FOUND).message("test").build());
    RestResponse<TaskSelectorMap> restResponse =
        RESOURCES.client()
            .target(String.format("/delegate-task-selector-map/%s/task-selectors?accountId=%s&selector=%s",
                taskSelectorMapUuid, ACCOUNT_ID, TAG_NAME))
            .request()
            .post(null, new GenericType<RestResponse<TaskSelectorMap>>() {});
    verify(taskSelectorMapService, atLeastOnce()).addTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldDeleteTaskSelector() {
    String taskSelectorMapUuid = generateUuid();
    TaskSelectorMap newTaskSelector =
        TaskSelectorMap.builder().uuid(taskSelectorMapUuid).accountId(ACCOUNT_ID).selectors(singleton("new")).build();
    when(taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME))
        .thenReturn(newTaskSelector);
    RestResponse<TaskSelectorMap> restResponse =
        RESOURCES.client()
            .target(String.format("/delegate-task-selector-map/%s/task-selectors?accountId=%s&selector=%s",
                taskSelectorMapUuid, ACCOUNT_ID, TAG_NAME))
            .request()
            .delete(new GenericType<RestResponse<TaskSelectorMap>>() {});
    verify(taskSelectorMapService, atLeastOnce()).removeTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME);
    assertThat(restResponse.getResource()).isEqualTo(newTaskSelector);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldDeleteaskSelectorMapThrowNotFound() {
    String taskSelectorMapUuid = generateUuid();
    thrown.expect(Matchers.hasProperty(
        "response", Matchers.hasProperty("status", CoreMatchers.is(HttpStatus.NOT_FOUND.value()))));

    when(taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME))
        .thenThrow(NoResultFoundException.newBuilder().code(ErrorCode.RESOURCE_NOT_FOUND).message("test").build());
    RestResponse<TaskSelectorMap> restResponse =
        RESOURCES.client()
            .target(String.format("/delegate-task-selector-map/%s/task-selectors?accountId=%s&selector=%s",
                taskSelectorMapUuid, ACCOUNT_ID, TAG_NAME))
            .request()
            .delete(new GenericType<RestResponse<TaskSelectorMap>>() {});
    verify(taskSelectorMapService, atLeastOnce()).removeTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldDeleteTaskSelectorCompletely() {
    String taskSelectorMapUuid = generateUuid();
    when(taskSelectorMapService.removeTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME)).thenReturn(null);
    RestResponse<TaskSelectorMap> restResponse =
        RESOURCES.client()
            .target(String.format("/delegate-task-selector-map/%s/task-selectors?accountId=%s&selector=%s",
                taskSelectorMapUuid, ACCOUNT_ID, TAG_NAME))
            .request()
            .delete(new GenericType<RestResponse<TaskSelectorMap>>() {});
    verify(taskSelectorMapService, atLeastOnce()).removeTaskSelector(ACCOUNT_ID, taskSelectorMapUuid, TAG_NAME);
    assertThat(restResponse.getResource()).isNull();
  }
}
