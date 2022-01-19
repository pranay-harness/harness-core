/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.rule.Owner;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import okhttp3.Call;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8EventHandlerTest extends CategoryTest {
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  @Mock private ApiClientFactory apiClientFactory;
  @Mock private ApiClient k8sApiClient;
  @Mock private Call k8sApiCall;

  @InjectMocks private K8EventHandler k8EventHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void stopEventWatch() throws IOException {
    Watch<V1Event> watch = mock(Watch.class);
    doNothing().when(watch).close();
    k8EventHandler.stopEventWatch(watch);
    verify(watch).close();
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void stopEventWatchWithIOError() throws IOException {
    Watch<V1Event> watch = mock(Watch.class);
    doThrow(IOException.class).when(watch).close();
    k8EventHandler.stopEventWatch(watch);
    verify(watch).close();
  }
}
