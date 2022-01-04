package io.harness.cdng.helm;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.helm.beans.NativeHelmExecutionPassThroughData;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.helm.HelmCommandRequestNG;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;

import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public abstract class AbstractHelmStepExecutorTestBase extends CategoryTest {
  @Mock protected NativeHelmStepHelper nativeHelmStepHelper;

  @Mock protected InfrastructureOutcome infrastructureOutcome;
  @Mock protected K8sInfraDelegateConfig infraDelegateConfig;
  @Mock protected CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock protected StoreConfig storeConfig;

  protected HelmChartManifestOutcome manifestOutcome;
  protected final String accountId = "accountId";
  protected final String releaseName = "releaseName";
  protected final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
  protected final HelmChartManifestDelegateConfig manifestDelegateConfig =
      HelmChartManifestDelegateConfig.builder().build();
  protected final UnitProgressData unitProgressData = UnitProgressData.builder().build();

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);

    manifestOutcome = HelmChartManifestOutcome.builder()
                          .skipResourceVersioning(ParameterField.createValueField(true))
                          .store(storeConfig)
                          .build();
    doReturn(infraDelegateConfig).when(nativeHelmStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
    doReturn(manifestDelegateConfig).when(nativeHelmStepHelper).getManifestDelegateConfig(manifestOutcome, ambiance);
    doReturn(releaseName).when(nativeHelmStepHelper).getReleaseName(ambiance, infrastructureOutcome);
    doReturn(TaskChainResponse.builder().chainEnd(true).build())
        .when(nativeHelmStepHelper)
        .startChainLink(any(), any(), any());
    when(cdFeatureFlagHelper.isEnabled(any(), any())).thenReturn(true);
  }

  protected <T extends HelmCommandRequestNG> T executeTask(
      StepElementParameters stepElementParameters, Class<T> requestType) {
    NativeHelmExecutionPassThroughData passThroughData =
        NativeHelmExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build();
    getHelmStepExecutor().executeHelmTask(
        manifestOutcome, ambiance, stepElementParameters, emptyList(), passThroughData, true, unitProgressData);
    ArgumentCaptor<T> requestCaptor = ArgumentCaptor.forClass(requestType);
    verify(nativeHelmStepHelper, times(1))
        .queueNativeHelmTask(eq(stepElementParameters), requestCaptor.capture(), eq(ambiance), eq(passThroughData));
    return requestCaptor.getValue();
  }

  protected abstract NativeHelmStepExecutor getHelmStepExecutor();
}
