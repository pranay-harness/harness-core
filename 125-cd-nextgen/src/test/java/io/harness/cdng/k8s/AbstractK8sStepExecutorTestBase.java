package io.harness.cdng.k8s;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.steps.shellScript.beans.InfrastructureOutcome;
import io.harness.steps.shellScript.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.pms.contracts.ambiance.Ambiance;

import io.harness.steps.shellScript.k8s.K8sStepExecutor;
import io.harness.steps.shellScript.k8s.K8sStepHelper;
import io.harness.steps.shellScript.k8s.K8sStepParameters;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public abstract class AbstractK8sStepExecutorTestBase extends CategoryTest {
  @Mock protected K8sStepHelper k8sStepHelper;

  @Mock protected InfrastructureOutcome infrastructureOutcome;
  @Mock protected K8sInfraDelegateConfig infraDelegateConfig;
  @Mock protected StoreConfig storeConfig;

  protected K8sManifestOutcome manifestOutcome;
  protected final String accountId = "accountId";
  protected final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
  protected final K8sManifestDelegateConfig manifestDelegateConfig = K8sManifestDelegateConfig.builder().build();

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);

    manifestOutcome = K8sManifestOutcome.builder().skipResourceVersioning(true).store(storeConfig).build();
    doReturn(infraDelegateConfig).when(k8sStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
    doReturn(manifestDelegateConfig).when(k8sStepHelper).getManifestDelegateConfig(manifestOutcome, ambiance);
    doReturn(true).when(k8sStepHelper).getSkipResourceVersioning(manifestOutcome);
  }

  protected <T extends K8sDeployRequest> T executeTask(K8sStepParameters stepParameters, Class<T> requestType) {
    getK8sStepExecutor().executeK8sTask(manifestOutcome, ambiance, stepParameters, emptyList(), infrastructureOutcome);
    ArgumentCaptor<T> requestCaptor = ArgumentCaptor.forClass(requestType);
    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepParameters), requestCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    return requestCaptor.getValue();
  }

  protected abstract K8sStepExecutor getK8sStepExecutor();
}
