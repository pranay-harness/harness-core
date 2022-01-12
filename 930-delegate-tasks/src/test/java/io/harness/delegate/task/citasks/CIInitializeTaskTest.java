/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.ci.k8s.CIK8InitializeTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.rule.Owner;

import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CIInitializeTaskTest extends CategoryTest {
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  @Mock @Named(CITaskConstants.INIT_K8) private CIInitializeTaskHandler ciK8InitializeTaskHandler;
  @Mock @Named(CITaskConstants.INIT_VM) private CIInitializeTaskHandler ciVmInitializeTaskHandler;

  @InjectMocks
  private CIInitializeTask task =
      new CIInitializeTask(DelegateTaskPackage.builder()
                               .delegateId("delegateid")
                               .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                               .build(),
          null, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithTaskParams() {
    CIInitializeTaskParams params = CIK8InitializeTaskParams.builder().build();
    K8sTaskExecutionResponse response = mock(K8sTaskExecutionResponse.class);
    when(ciK8InitializeTaskHandler.executeTaskInternal(params, logStreamingTaskClient, task.getTaskId()))
        .thenReturn(response);
    assertEquals(task.run(params), response);
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void runWithObjectParams() {
    CIInitializeTaskParams taskParams = CIK8InitializeTaskParams.builder().build();
    List<Object> params = new ArrayList<>();
    params.add(taskParams);

    task.run(params.toArray());
  }
}
