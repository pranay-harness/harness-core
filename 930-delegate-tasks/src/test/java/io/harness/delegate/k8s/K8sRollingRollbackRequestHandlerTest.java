/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.k8s.beans.K8sRollingRollbackHandlerConfig;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class K8sRollingRollbackRequestHandlerTest extends CategoryTest {
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @Mock private K8sRollingRollbackBaseHandler k8sRollingRollbackBaseHandler;
  @Mock private ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;

  @InjectMocks private K8sRollingRollbackRequestHandler k8sRollingRollbackRequestHandler;

  @Mock private K8sInfraDelegateConfig k8sInfraDelegateConfig;
  @Mock private LogCallback logCallback;
  @Mock private KubernetesConfig kubernetesConfig;

  private K8sRollingRollbackHandlerConfig rollbackHandlerConfig;
  private K8sRollingRollbackDeployRequest k8sRollingRollbackDeployRequest;
  private K8sDelegateTaskParams k8sDelegateTaskParams;

  private final Integer releaseNumber = 2;
  private final Integer timeoutIntervalInMin = 10;
  private final String releaseName = "releaseName";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(logCallback)
        .when(k8sTaskHelperBase)
        .getLogCallback(eq(logStreamingTaskClient), anyString(), anyBoolean(), any());
    doReturn(kubernetesConfig)
        .when(containerDeploymentDelegateBaseHelper)
        .createKubernetesConfig(k8sInfraDelegateConfig);

    rollbackHandlerConfig = k8sRollingRollbackRequestHandler.getRollbackHandlerConfig();
    k8sRollingRollbackDeployRequest = K8sRollingRollbackDeployRequest.builder()
                                          .k8sInfraDelegateConfig(k8sInfraDelegateConfig)
                                          .releaseName(releaseName)
                                          .releaseNumber(releaseNumber)
                                          .timeoutIntervalInMin(timeoutIntervalInMin)
                                          .build();
    k8sDelegateTaskParams = K8sDelegateTaskParams.builder().build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRollbackSuccess() throws Exception {
    K8sDeployResponse response = k8sRollingRollbackRequestHandler.executeTaskInternal(
        k8sRollingRollbackDeployRequest, k8sDelegateTaskParams, logStreamingTaskClient, null);

    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(rollbackHandlerConfig.getKubernetesConfig()).isSameAs(kubernetesConfig);
    assertThat(rollbackHandlerConfig.getClient()).isNotNull();
    verify(k8sRollingRollbackBaseHandler).init(rollbackHandlerConfig, releaseName, logCallback);
    verify(k8sRollingRollbackBaseHandler)
        .rollback(rollbackHandlerConfig, k8sDelegateTaskParams, releaseNumber, logCallback, emptySet(), true);
    verify(k8sRollingRollbackBaseHandler)
        .steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, timeoutIntervalInMin, logCallback);
    verify(k8sRollingRollbackBaseHandler).postProcess(rollbackHandlerConfig, releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRollbackInitFailed() throws Exception {
    RuntimeException thrownException = new RuntimeException("failed");
    doThrow(thrownException).when(k8sRollingRollbackBaseHandler).init(rollbackHandlerConfig, releaseName, logCallback);

    assertThatThrownBy(()
                           -> k8sRollingRollbackRequestHandler.executeTaskInternal(
                               k8sRollingRollbackDeployRequest, k8sDelegateTaskParams, logStreamingTaskClient, null))
        .isSameAs(thrownException);

    assertThat(rollbackHandlerConfig.getKubernetesConfig()).isSameAs(kubernetesConfig);
    assertThat(rollbackHandlerConfig.getClient()).isNotNull();
    verify(k8sRollingRollbackBaseHandler).init(rollbackHandlerConfig, releaseName, logCallback);
    verify(k8sRollingRollbackBaseHandler, never())
        .rollback(rollbackHandlerConfig, k8sDelegateTaskParams, releaseNumber, logCallback, emptySet(), true);
    verify(k8sRollingRollbackBaseHandler, never())
        .steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, timeoutIntervalInMin, logCallback);
    verify(k8sRollingRollbackBaseHandler, never()).postProcess(rollbackHandlerConfig, releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRollbackRollbackFailed() throws Exception {
    RuntimeException thrownException = new RuntimeException("error");
    doThrow(thrownException)
        .when(k8sRollingRollbackBaseHandler)
        .rollback(rollbackHandlerConfig, k8sDelegateTaskParams, releaseNumber, logCallback, emptySet(), true);

    assertThatThrownBy(()
                           -> k8sRollingRollbackRequestHandler.executeTaskInternal(
                               k8sRollingRollbackDeployRequest, k8sDelegateTaskParams, logStreamingTaskClient, null))
        .isSameAs(thrownException);

    assertThat(rollbackHandlerConfig.getKubernetesConfig()).isSameAs(kubernetesConfig);
    assertThat(rollbackHandlerConfig.getClient()).isNotNull();
    verify(k8sRollingRollbackBaseHandler).init(rollbackHandlerConfig, releaseName, logCallback);
    verify(k8sRollingRollbackBaseHandler)
        .rollback(rollbackHandlerConfig, k8sDelegateTaskParams, releaseNumber, logCallback, emptySet(), true);
    verify(k8sRollingRollbackBaseHandler, never())
        .steadyStateCheck(rollbackHandlerConfig, k8sDelegateTaskParams, timeoutIntervalInMin, logCallback);
    verify(k8sRollingRollbackBaseHandler, never()).postProcess(rollbackHandlerConfig, releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalInvalidParamType() {
    K8sDeployRequest k8sDeployRequest = mock(K8sDeployRequest.class);

    assertThatThrownBy(()
                           -> k8sRollingRollbackRequestHandler.executeTaskInternal(
                               k8sDeployRequest, k8sDelegateTaskParams, logStreamingTaskClient, null))
        .isInstanceOf(InvalidArgumentsException.class);
  }
}
