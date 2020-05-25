package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.Collections;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ManifestHelper.class, K8sApplyTaskHandler.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class K8sApplyTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @InjectMocks private K8sApplyTaskHandler k8sApplyTaskHandler;

  @Before
  public void setup() {
    PowerMockito.mockStatic(ManifestHelper.class);
    doReturn(mock(ExecutionLogCallback.class)).when(k8sTaskHelper).getExecutionLogCallback(any(), anyString());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsSkipped() throws Exception {
    K8sApplyTaskParameters k8sApplyTaskParameters =
        K8sApplyTaskParameters.builder().skipDryRun(true).filePaths("abc/xyz.yaml").build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(k8sTaskHelper.renderTemplateForApply(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(asList(ManifestFile.builder().build()));
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sApplyTaskHandler.init(k8sApplyTaskParameters, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1))
        .renderTemplateForApply(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    K8sApplyTaskParameters k8sApplyTaskParameters =
        K8sApplyTaskParameters.builder().skipDryRun(false).filePaths("abc/xyz.yaml").build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(KubernetesConfig.builder().build());
    when(k8sTaskHelper.renderTemplateForApply(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(asList(ManifestFile.builder().build()));
    doNothing().when(k8sTaskHelper).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelper.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sApplyTaskHandler.init(k8sApplyTaskParameters, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelper, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).readManifests(any(), any());
    verify(k8sTaskHelper, times(1))
        .renderTemplateForApply(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(k8sTaskHelper, times(1)).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void invalidTypeOfTaskParams() {
    assertThatExceptionOfType(InvalidArgumentsException.class)
        .isThrownBy(() -> k8sApplyTaskHandler.executeTaskInternal(null, null))
        .withMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void failureInFetchingManifestFiles() {
    doReturn(false)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));

    final K8sTaskExecutionResponse response =
        k8sApplyTaskHandler.executeTask(K8sApplyTaskParameters.builder().releaseName("release-name").build(),
            K8sDelegateTaskParams.builder().workingDirectory(".").build());

    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getK8sTaskResponse()).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void noFileSpecifiedInApply() {
    boolean success;
    success = k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));
    assertThat(success).isFalse();

    success = k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));
    assertThat(success).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void readAllFilesSpecifiedInApply() throws Exception {
    doReturn(new KubernetesConfig())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));

    k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("a,b,c").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .renderTemplateForApply(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("a", "b", "c")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));

    k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("a").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .renderTemplateForApply(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("a")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));

    k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("b ,").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .renderTemplateForApply(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("b")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void invalidManifestFiles() throws Exception {
    doReturn(new KubernetesConfig())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class));

    doReturn(asList(ManifestFile.builder().build()))
        .when(k8sTaskHelper)
        .renderTemplateForApply(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("a", "b", "c")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));

    doThrow(new KubernetesYamlException("reason"))
        .when(k8sTaskHelper)
        .readManifests(anyList(), any(ExecutionLogCallback.class));

    final boolean success = k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("a,b,c").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    assertThat(success).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void initFailure() throws Exception {
    K8sApplyTaskHandler handler = Mockito.spy(k8sApplyTaskHandler);

    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    doReturn(false).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    final K8sTaskExecutionResponse response =
        handler.executeTaskInternal(K8sApplyTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build());

    verify(handler, times(1))
        .init(any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepare() {
    List<KubernetesResource> resources = asList(KubernetesResource.builder().build());
    List<KubernetesResource> workloads = asList(KubernetesResource.builder().build());
    PowerMockito.when(ManifestHelper.getWorkloadsForApplyState(resources)).thenReturn(workloads);
    PowerMockito.when(ManifestHelper.getWorkloadsForApplyState(EMPTY_LIST)).thenReturn(EMPTY_LIST);
    Reflect.on(k8sApplyTaskHandler).set("resources", EMPTY_LIST);

    assertThat(k8sApplyTaskHandler.prepare(mock(ExecutionLogCallback.class))).isTrue();
    assertThat(Reflect.on(k8sApplyTaskHandler).<List>get("resources")).isEmpty();

    Reflect.on(k8sApplyTaskHandler).set("resources", resources);
    assertThat(k8sApplyTaskHandler.prepare(mock(ExecutionLogCallback.class))).isTrue();
    assertThat(Reflect.on(k8sApplyTaskHandler).<List>get("resources")).hasSize(1);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareFailure() {
    PowerMockito.when(ManifestHelper.getWorkloadsForApplyState(anyList())).thenThrow(new RuntimeException());
    assertThat(k8sApplyTaskHandler.prepare(mock(ExecutionLogCallback.class))).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void failureInApplyingManifestFiles() throws Exception {
    K8sApplyTaskHandler handler = spy(k8sApplyTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepare(any(ExecutionLogCallback.class));
    doReturn(false)
        .when(k8sTaskHelper)
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    final K8sTaskExecutionResponse response =
        k8sApplyTaskHandler.executeTask(K8sApplyTaskParameters.builder().releaseName("release-name").build(),
            K8sDelegateTaskParams.builder().workingDirectory(".").build());

    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getK8sTaskResponse()).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void applyManifestFiles() throws Exception {
    K8sApplyTaskHandler handler = spy(k8sApplyTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepare(any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .applyManifests(
            any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true)
        .when(k8sTaskHelper)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class));
    Reflect.on(handler).set("workloads",
        asList(KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build(),
            KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build()));

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    final K8sTaskExecutionResponse response =
        handler.executeTask(K8sApplyTaskParameters.builder()
                                .releaseName("release-name")
                                .skipSteadyStateCheck(false)
                                .k8sClusterConfig(K8sClusterConfig.builder().namespace("default").build())
                                .build(),
            K8sDelegateTaskParams.builder().workingDirectory(".").build());

    verify(k8sTaskHelper, times(1))
        .doStatusCheckForAllResources(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            anyString(), any(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .describe(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    @SuppressWarnings("unchecked")
    final List<KubernetesResourceId> capturedResources = (List<KubernetesResourceId>) captor.getValue();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getK8sTaskResponse()).isNotNull();
    assertThat(capturedResources).hasSize(2);
  }
}
