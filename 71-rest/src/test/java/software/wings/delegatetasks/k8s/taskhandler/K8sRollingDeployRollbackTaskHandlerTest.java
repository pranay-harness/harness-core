package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.k8s.model.Release.Status.Failed;
import static io.harness.k8s.model.Release.Status.Succeeded;
import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.k8s.K8sTestHelper.buildProcessResult;
import static software.wings.delegatetasks.k8s.K8sTestHelper.buildRelease;
import static software.wings.delegatetasks.k8s.K8sTestHelper.buildReleaseMultipleManagedWorkloads;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.harness.category.element.UnitTests;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutUndoCommand;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;

import java.net.URL;

@RunWith(PowerMockRunner.class)
@PrepareForTest({K8sTaskHelper.class, Utils.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class K8sRollingDeployRollbackTaskHandlerTest extends WingsBaseTest {
  @Mock private ReleaseHistory releaseHistory;
  @Mock private K8sTaskHelper taskHelper;
  @Mock private ExecutionLogCallback logCallback;

  @InjectMocks private K8sRollingDeployRollbackTaskHandler k8sRollingDeployRollbackTaskHandler;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRollbackForDC() throws Exception {
    rollbackUtil("/k8s/deployment-config.yaml",
        "oc --kubeconfig=config-path rollout undo DeploymentConfig/test-dc --namespace=default --to-revision=1");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRollbackForDeployment() throws Exception {
    rollbackUtil("/k8s/deployment.yaml",
        "kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment --to-revision=1");
  }

  private void rollbackUtil(String manifestFilePath, String expectedOutput) throws Exception {
    K8sRollingDeployRollbackTaskParameters k8sRollingDeployRollbackTaskParameters =
        K8sRollingDeployRollbackTaskParameters.builder().build();
    K8sDelegateTaskParams k8sDelegateTaskParams =
        K8sDelegateTaskParams.builder().kubectlPath("kubectl").ocPath("oc").kubeconfigPath("config-path").build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();

    URL url = this.getClass().getResource(manifestFilePath);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource kubernetesResource = ManifestHelper.processYaml(fileContents).get(0);

    Release release = Release.builder()
                          .resources(asList(kubernetesResource.getResourceId()))
                          .number(2)
                          .managedWorkloads(asList(Release.KubernetesResourceIdRevision.builder()
                                                       .workload(kubernetesResource.getResourceId())
                                                       .revision("2")
                                                       .build()))
                          .build();
    Release previousEligibleRelease = Release.builder()
                                          .resources(asList(kubernetesResource.getResourceId()))
                                          .number(1)
                                          .managedWorkloads(asList(Release.KubernetesResourceIdRevision.builder()
                                                                       .workload(kubernetesResource.getResourceId())
                                                                       .revision("1")
                                                                       .build()))
                                          .build();

    when(releaseHistory.getPreviousRollbackEligibleRelease(anyInt())).thenReturn(previousEligibleRelease);
    on(k8sRollingDeployRollbackTaskHandler).set("releaseHistory", releaseHistory);
    on(k8sRollingDeployRollbackTaskHandler).set("release", release);
    on(k8sRollingDeployRollbackTaskHandler).set("client", Kubectl.client("kubectl", "config-path"));

    PowerMockito.mockStatic(Utils.class);
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("kubectl")).thenReturn("kubectl");
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("oc")).thenReturn("oc");
    PowerMockito.when(Utils.encloseWithQuotesIfNeeded("config-path")).thenReturn("config-path");

    ProcessResult processResult = new ProcessResult(0, new ProcessOutput("".getBytes()));
    PowerMockito.when(Utils.executeScript(anyString(), anyString(), any(), any())).thenReturn(processResult);

    boolean rollback = k8sRollingDeployRollbackTaskHandler.rollback(
        k8sRollingDeployRollbackTaskParameters, k8sDelegateTaskParams, executionLogCallback);
    assertThat(rollback).isTrue();

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    PowerMockito.verifyStatic(Utils.class);
    Utils.executeScript(any(), captor.capture(), any(), any());
    assertThat(captor.getValue()).isEqualTo(expectedOutput);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void rollback() throws Exception {
    testRollBackReleaseIsNull();
    testRollBackIfNoManagedWorkload();
    testRollBackToSpecificRelease();
  }

  private void testRollBackToSpecificRelease() throws Exception {
    rollback1ManagedWorkload();
    rollbackMultipleWorkloads();
  }

  private void rollbackMultipleWorkloads() throws Exception {
    K8sRollingDeployRollbackTaskHandler spyHandler = spy(K8sRollingDeployRollbackTaskHandler.class);
    on(spyHandler).set("client", Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(0)).when(spyHandler).runK8sExecutable(any(), any(), any());
    doReturn(buildProcessResult(0)).when(spyHandler).executeScript(any(), anyString(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Failed));
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    releaseHistory.getReleases().add(buildReleaseMultipleManagedWorkloads(Succeeded));
    on(spyHandler).set("release", releaseHistory.getLatestRelease());
    on(spyHandler).set("releaseHistory", releaseHistory);

    final boolean success =
        spyHandler.rollback(K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(2).build(),
            K8sDelegateTaskParams.builder().kubeconfigPath("kubeconfig").build(), logCallback);

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(spyHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());
    verify(spyHandler, times(1)).executeScript(any(), stringArgumentCaptor.capture(), any(), any());

    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment --to-revision=2");
    assertThat(success).isTrue();

    String command = stringArgumentCaptor.getValue();
    assertThat(command).isEqualTo(
        "oc --kubeconfig=kubeconfig rollout undo DeploymentConfig/test-dc --namespace=default --to-revision=2");
  }

  private void rollback1ManagedWorkload() throws Exception {
    K8sRollingDeployRollbackTaskHandler spyHandler = spy(K8sRollingDeployRollbackTaskHandler.class);
    on(spyHandler).set("client", Kubectl.client("kubectl", "config-path"));
    doReturn(buildProcessResult(0)).when(spyHandler).runK8sExecutable(any(), any(), any());

    ReleaseHistory releaseHistory = ReleaseHistory.createNew();
    releaseHistory.getReleases().add(buildRelease(Failed, 2));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 1));
    releaseHistory.getReleases().add(buildRelease(Succeeded, 0));
    on(spyHandler).set("release", releaseHistory.getLatestRelease());
    on(spyHandler).set("releaseHistory", releaseHistory);

    final boolean success =
        spyHandler.rollback(K8sRollingDeployRollbackTaskParameters.builder().releaseNumber(2).build(),
            K8sDelegateTaskParams.builder().build(), logCallback);

    ArgumentCaptor<RolloutUndoCommand> captor = ArgumentCaptor.forClass(RolloutUndoCommand.class);
    verify(spyHandler, times(1)).runK8sExecutable(any(), any(), captor.capture());

    RolloutUndoCommand rolloutUndoCommand = captor.getValue();
    assertThat(rolloutUndoCommand.command())
        .isEqualTo("kubectl --kubeconfig=config-path rollout undo Deployment/nginx-deployment");
    assertThat(success).isTrue();
  }

  private void testRollBackIfNoManagedWorkload() throws Exception {
    on(k8sRollingDeployRollbackTaskHandler).set("release", new Release());
    final boolean success = k8sRollingDeployRollbackTaskHandler.rollback(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build(), logCallback);
    assertThat(success).isTrue();
  }

  private void testRollBackReleaseIsNull() throws Exception {
    final boolean success = k8sRollingDeployRollbackTaskHandler.rollback(
        K8sRollingDeployRollbackTaskParameters.builder().build(), K8sDelegateTaskParams.builder().build(), logCallback);

    assertThat(success).isTrue();
  }
}
