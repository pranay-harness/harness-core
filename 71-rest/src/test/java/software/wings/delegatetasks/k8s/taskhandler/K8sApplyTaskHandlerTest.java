package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
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

import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.delegatetasks.k8s.K8sTestHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class K8sApplyTaskHandlerTest extends WingsBaseTest {
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private K8sTaskHelper k8sTaskHelper;
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @InjectMocks private K8sApplyTaskHandler k8sApplyTaskHandler;

  @Captor ArgumentCaptor<List<KubernetesResource>> kubernetesResourceListCaptor;

  @Before
  public void setup() {
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

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), eq(false)))
        .thenReturn(KubernetesConfig.builder().build());
    when(k8sTaskHelper.renderTemplateForGivenFiles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(asList(FileData.builder().build()));
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sApplyTaskHandler.init(k8sApplyTaskParameters, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(0)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1))
        .getResourcesFromManifests(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), eq(false));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDryRunIsNotSkipped() throws Exception {
    K8sApplyTaskParameters k8sApplyTaskParameters =
        K8sApplyTaskParameters.builder().skipDryRun(false).filePaths("abc/xyz.yaml").build();
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class), eq(false)))
        .thenReturn(KubernetesConfig.builder().build());
    when(k8sTaskHelper.renderTemplateForGivenFiles(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(asList(FileData.builder().build()));
    doNothing().when(k8sTaskHelperBase).setNamespaceToKubernetesResourcesIfRequired(any(), any());
    when(k8sTaskHelperBase.readManifests(any(), any())).thenReturn(Collections.emptyList());

    k8sApplyTaskHandler.init(k8sApplyTaskParameters, delegateTaskParams, executionLogCallback);
    verify(k8sTaskHelperBase, times(1)).dryRunManifests(any(), any(), any(), any());
    verify(k8sTaskHelper, times(1))
        .getResourcesFromManifests(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(containerDeploymentDelegateHelper, times(1)).getKubernetesConfig(any(K8sClusterConfig.class), eq(false));
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
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());

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
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class), eq(false));

    k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("a,b,c").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .getResourcesFromManifests(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("a", "b", "c")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));

    k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("a").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .getResourcesFromManifests(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("a")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));

    k8sApplyTaskHandler.init(K8sApplyTaskParameters.builder().filePaths("b ,").build(),
        K8sDelegateTaskParams.builder().build(), Mockito.mock(ExecutionLogCallback.class));

    verify(k8sTaskHelper, times(1))
        .getResourcesFromManifests(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class), anyString(),
            eq(asList("b")), anyList(), anyString(), anyString(), any(ExecutionLogCallback.class),
            any(K8sTaskParameters.class));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void invalidManifestFiles() throws Exception {
    doReturn(KubernetesConfig.builder().build())
        .when(containerDeploymentDelegateHelper)
        .getKubernetesConfig(any(K8sClusterConfig.class), eq(false));

    doReturn(asList(ManifestFile.builder().build()))
        .when(k8sTaskHelper)
        .renderTemplateForGivenFiles(any(K8sDelegateTaskParams.class), any(K8sDelegateManifestConfig.class),
            anyString(), eq(asList("a", "b", "c")), anyList(), anyString(), anyString(),
            any(ExecutionLogCallback.class), any(K8sTaskParameters.class));

    doThrow(new KubernetesYamlException("reason"))
        .when(k8sTaskHelperBase)
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
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
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
  public void prepare() throws IOException {
    List<KubernetesResource> resources = asList(K8sTestHelper.deployment(), K8sTestHelper.configMap());
    Reflect.on(k8sApplyTaskHandler).set("resources", EMPTY_LIST);

    assertThat(k8sApplyTaskHandler.prepare(mock(ExecutionLogCallback.class), mock(K8sApplyTaskParameters.class)))
        .isTrue();
    assertThat(Reflect.on(k8sApplyTaskHandler).<List>get("resources")).isEmpty();

    Reflect.on(k8sApplyTaskHandler).set("resources", resources);
    assertThat(k8sApplyTaskHandler.prepare(mock(ExecutionLogCallback.class), mock(K8sApplyTaskParameters.class)))
        .isTrue();
    assertThat(Reflect.on(k8sApplyTaskHandler).<List>get("workloads")).hasSize(1);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void prepareWorkloadsFound() throws IOException {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    K8sApplyTaskParameters k8sApplyTaskParameters = mock(K8sApplyTaskParameters.class);
    KubernetesResource customWorkload = ManifestHelper
                                            .processYaml("apiVersion: apps/v1\n"
                                                + "kind: Foo\n"
                                                + "metadata:\n"
                                                + "  name: foo\n"
                                                + "  annotations:\n"
                                                + "    harness.io/managed-workload: true\n"
                                                + "spec:\n"
                                                + "  replicas: 1")
                                            .get(0);
    List<KubernetesResource> resources = asList(K8sTestHelper.deployment(), K8sTestHelper.configMap(), customWorkload);
    Reflect.on(k8sApplyTaskHandler).set("resources", resources);

    boolean success = k8sApplyTaskHandler.prepare(executionLogCallback, k8sApplyTaskParameters);
    assertThat(success).isTrue();
    assertThat(Reflect.on(k8sApplyTaskHandler).<List>get("workloads")).hasSize(1);
    assertThat(Reflect.on(k8sApplyTaskHandler).<List>get("customWorkloads")).hasSize(1);
    verify(k8sTaskHelperBase, times(2)).getResourcesInTableFormat(kubernetesResourceListCaptor.capture());
    List<List<KubernetesResource>> kubernetesResourcesList = kubernetesResourceListCaptor.getAllValues();
    // first time it retrieves all
    List<KubernetesResource> workloadsFound = kubernetesResourcesList.get(0);
    assertThat(workloadsFound.size()).isEqualTo(3);
    // second time workload and custom workloads are filtered
    workloadsFound = kubernetesResourcesList.get(1);
    // one workload and one custom workload
    assertThat(workloadsFound.size()).isEqualTo(2);
    assertThat(workloadsFound.get(0).getResourceId().getName()).isEqualTo("nginx-deployment");
    assertThat(workloadsFound.get(1).getResourceId().getName()).isEqualTo("foo");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void prepareFailure() {
    //    PowerMockito.when(ManifestHelper.getWorkloadsForApplyState(anyList())).thenThrow(new RuntimeException());
    assertThat(k8sApplyTaskHandler.prepare(mock(ExecutionLogCallback.class), mock(K8sApplyTaskParameters.class)))
        .isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void failureInApplyingManifestFiles() throws Exception {
    K8sApplyTaskHandler handler = spy(k8sApplyTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepare(any(ExecutionLogCallback.class), any(K8sApplyTaskParameters.class));
    doReturn(false)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());

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
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepare(any(ExecutionLogCallback.class), any(K8sApplyTaskParameters.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean(), anyLong());

    Reflect.on(handler).set("workloads",
        asList(KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build(),
            KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build()));

    Reflect.on(handler).set("customWorkloads",
        asList(KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build(),
            KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build()));

    Reflect.on(handler).set("resources",
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

    verify(k8sTaskHelperBase, times(1))
        .doStatusCheckForAllResources(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            anyString(), any(ExecutionLogCallback.class), anyBoolean());

    verify(k8sTaskHelperBase, times(1))
        .describe(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    @SuppressWarnings("unchecked")
    final List<KubernetesResourceId> capturedResources = (List<KubernetesResourceId>) captor.getValue();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getK8sTaskResponse()).isNotNull();
    assertThat(capturedResources).hasSize(2);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void applyManifestFilesWithCrd() throws Exception {
    K8sApplyTaskHandler handler = spy(k8sApplyTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepare(any(ExecutionLogCallback.class), any(K8sApplyTaskParameters.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), eq(false));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), eq(true), anyLong());

    Reflect.on(handler).set("workloads",
        asList(KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build(),
            KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build()));

    List<KubernetesResource> managedResources = ManifestHelper.processYaml("apiVersion: apps/v1\n"
        + "kind: Foo\n"
        + "metadata:\n"
        + "  name: deployment\n"
        + "  annotations:\n"
        + "    harness.io/managed-workload: true\n"
        + "spec:\n"
        + "  replicas: 1");

    Reflect.on(handler).set("resources", managedResources);
    Reflect.on(handler).set("customWorkloads", managedResources);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    final K8sTaskExecutionResponse response =
        handler.executeTask(K8sApplyTaskParameters.builder()
                                .releaseName("release-name")
                                .skipSteadyStateCheck(false)
                                .k8sClusterConfig(K8sClusterConfig.builder().namespace("default").build())
                                .build(),
            K8sDelegateTaskParams.builder().workingDirectory(".").build());

    verify(k8sTaskHelperBase, times(1))
        .doStatusCheckForAllCustomResources(any(Kubectl.class), captor.capture(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), eq(true), anyLong());

    verify(k8sTaskHelperBase, times(1))
        .describe(any(Kubectl.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));

    @SuppressWarnings("unchecked")
    final List<KubernetesResourceId> capturedResources = (List<KubernetesResourceId>) captor.getValue();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getK8sTaskResponse()).isNotNull();
    assertThat(capturedResources).hasSize(1);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testFailCrdStatusCheck() throws Exception {
    K8sApplyTaskHandler handler = spy(k8sApplyTaskHandler);
    doReturn(true)
        .when(k8sTaskHelper)
        .fetchManifestFilesAndWriteToDirectory(
            any(K8sDelegateManifestConfig.class), anyString(), any(ExecutionLogCallback.class), anyLong());
    doReturn(true).when(handler).init(
        any(K8sApplyTaskParameters.class), any(K8sDelegateTaskParams.class), any(ExecutionLogCallback.class));
    doReturn(true).when(handler).prepare(any(ExecutionLogCallback.class), any(K8sApplyTaskParameters.class));
    doReturn(true)
        .when(k8sTaskHelperBase)
        .applyManifests(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(true)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class), anyString(),
            any(ExecutionLogCallback.class), anyBoolean());
    doReturn(false)
        .when(k8sTaskHelperBase)
        .doStatusCheckForAllCustomResources(any(Kubectl.class), anyList(), any(K8sDelegateTaskParams.class),
            any(ExecutionLogCallback.class), eq(true), anyLong());

    doReturn(K8sTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).build())
        .when(k8sTaskHelper)
        .getK8sTaskExecutionResponse(any(K8sTaskResponse.class), any(CommandExecutionStatus.class));

    Reflect.on(handler).set("workloads",
        asList(KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build(),
            KubernetesResource.builder().resourceId(KubernetesResourceId.builder().build()).build()));

    List<KubernetesResource> managedResources = ManifestHelper.processYaml("apiVersion: apps/v1\n"
        + "kind: Foo\n"
        + "metadata:\n"
        + "  name: deployment\n"
        + "  annotations:\n"
        + "    harness.io/managed-workload: true\n"
        + "spec:\n"
        + "  replicas: 1");

    Reflect.on(handler).set("resources", managedResources);
    Reflect.on(handler).set("customWorkloads", managedResources);

    ArgumentCaptor<CommandExecutionStatus> captor = ArgumentCaptor.forClass(CommandExecutionStatus.class);
    final K8sTaskExecutionResponse response =
        handler.executeTask(K8sApplyTaskParameters.builder()
                                .releaseName("release-name")
                                .skipSteadyStateCheck(false)
                                .k8sClusterConfig(K8sClusterConfig.builder().namespace("default").build())
                                .build(),
            K8sDelegateTaskParams.builder().workingDirectory(".").build());

    verify(k8sTaskHelper, times(1)).getK8sTaskExecutionResponse(any(K8sTaskResponse.class), captor.capture());

    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    @SuppressWarnings("unchecked") final CommandExecutionStatus capturedResources = captor.getValue();
    assertThat(capturedResources).isEqualTo(FAILURE);
  }
}
