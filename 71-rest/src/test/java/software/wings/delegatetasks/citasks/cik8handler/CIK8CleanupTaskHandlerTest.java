package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.CIK8CleanupTaskParams;
import software.wings.delegatetasks.citasks.CICleanupTaskHandler;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.KubernetesHelperService;

import java.util.List;

public class CIK8CleanupTaskHandlerTest extends WingsBaseTest {
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private CIK8CtlHandler kubeCtlHandler;
  @InjectMocks private CIK8CleanupTaskHandler cik8DeleteSetupTaskHandler;

  private static final String namespace = "default";
  private static final String podName = "pod";

  private CIK8CleanupTaskParams getTaskParams() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace(namespace).build();
    List<EncryptedDataDetail> encryptionDetails = mock(List.class);
    return CIK8CleanupTaskParams.builder()
        .encryptionDetails(encryptionDetails)
        .kubernetesConfig(kubernetesConfig)
        .podName(podName)
        .namespace(namespace)
        .build();
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithSuccess() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParams();

    when(kubernetesHelperService.getKubernetesClient(
             taskParams.getKubernetesConfig(), taskParams.getEncryptionDetails()))
        .thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenReturn(Boolean.TRUE);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithFailure() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParams();

    when(kubernetesHelperService.getKubernetesClient(
             taskParams.getKubernetesConfig(), taskParams.getEncryptionDetails()))
        .thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenReturn(Boolean.FALSE);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskWithDeleteException() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8CleanupTaskParams taskParams = getTaskParams();

    when(kubernetesHelperService.getKubernetesClient(
             taskParams.getKubernetesConfig(), taskParams.getEncryptionDetails()))
        .thenReturn(kubernetesClient);
    when(kubeCtlHandler.deletePod(kubernetesClient, podName, namespace)).thenThrow(KubernetesClientException.class);

    K8sTaskExecutionResponse response = cik8DeleteSetupTaskHandler.executeTaskInternal(taskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(CICleanupTaskHandler.Type.GCP_K8, cik8DeleteSetupTaskHandler.getType());
  }
}