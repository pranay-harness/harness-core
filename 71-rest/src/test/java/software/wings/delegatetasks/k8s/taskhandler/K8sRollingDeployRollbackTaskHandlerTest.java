package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.harness.category.element.UnitTests;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.rule.Owner;
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
@PowerMockIgnore("javax.net.*")
public class K8sRollingDeployRollbackTaskHandlerTest extends WingsBaseTest {
  @Mock private ReleaseHistory releaseHistory;

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
}
