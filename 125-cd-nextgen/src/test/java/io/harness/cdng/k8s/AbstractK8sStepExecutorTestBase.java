/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;

import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public abstract class AbstractK8sStepExecutorTestBase extends CategoryTest {
  @Mock protected K8sStepHelper k8sStepHelper;

  @Mock protected InfrastructureOutcome infrastructureOutcome;
  @Mock protected K8sInfraDelegateConfig infraDelegateConfig;
  @Mock protected StoreConfig storeConfig;

  protected K8sManifestOutcome manifestOutcome;
  protected final String accountId = "accountId";
  protected final String releaseName = "releaseName";
  protected final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
  protected final K8sManifestDelegateConfig manifestDelegateConfig = K8sManifestDelegateConfig.builder().build();
  protected final UnitProgressData unitProgressData = UnitProgressData.builder().build();

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);

    manifestOutcome = K8sManifestOutcome.builder()
                          .skipResourceVersioning(ParameterField.createValueField(true))
                          .store(storeConfig)
                          .build();
    doReturn(infraDelegateConfig).when(k8sStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
    doReturn(manifestDelegateConfig).when(k8sStepHelper).getManifestDelegateConfig(manifestOutcome, ambiance);
    doReturn(true).when(k8sStepHelper).getSkipResourceVersioning(manifestOutcome);
    doReturn(releaseName).when(k8sStepHelper).getReleaseName(ambiance, infrastructureOutcome);
    doReturn(TaskChainResponse.builder().chainEnd(true).build())
        .when(k8sStepHelper)
        .startChainLink(any(), any(), any());
  }

  protected <T extends K8sDeployRequest> T executeTask(
      StepElementParameters stepElementParameters, Class<T> requestType) {
    K8sExecutionPassThroughData passThroughData =
        K8sExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build();
    getK8sStepExecutor().executeK8sTask(
        manifestOutcome, ambiance, stepElementParameters, emptyList(), passThroughData, true, unitProgressData);
    ArgumentCaptor<T> requestCaptor = ArgumentCaptor.forClass(requestType);
    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepElementParameters), requestCaptor.capture(), eq(ambiance), eq(passThroughData));
    return requestCaptor.getValue();
  }

  protected abstract K8sStepExecutor getK8sStepExecutor();
}
