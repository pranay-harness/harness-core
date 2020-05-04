package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildGitSecretErrorTaskParams;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildImageSecretErrorTaskParams;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildPodCreateErrorTaskParams;

import io.fabric8.kubernetes.api.model.PodBuilder;
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
import software.wings.beans.GitConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.PodParams;
import software.wings.beans.container.ImageDetails;
import software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilder;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.KubernetesHelperService;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class CIK8BuildTaskHandlerTest extends WingsBaseTest {
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private CIK8CtlHandler kubeCtlHandler;
  @Mock private CIK8PodSpecBuilder podSpecBuilder;
  @InjectMocks private CIK8BuildTaskHandler cik8BuildTaskHandler;

  private static final String namespace = "default";

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithGitSecretError() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8BuildTaskParams cik8BuildTaskParams = buildGitSecretErrorTaskParams();

    when(kubernetesHelperService.getKubernetesClient(
             cik8BuildTaskParams.getKubernetesConfig(), cik8BuildTaskParams.getEncryptionDetails()))
        .thenReturn(kubernetesClient);
    doThrow(UnsupportedEncodingException.class)
        .when(kubeCtlHandler)
        .createGitSecret(kubernetesClient, namespace, cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig(),
            cik8BuildTaskParams.getGitFetchFilesConfig().getEncryptedDataDetails());

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithImageSecretError() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);

    CIK8BuildTaskParams cik8BuildTaskParams = buildImageSecretErrorTaskParams();
    KubernetesConfig kubernetesConfig = cik8BuildTaskParams.getKubernetesConfig();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetails imageDetails =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetails();

    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptionDetails)).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doThrow(KubernetesClientException.class)
        .when(kubeCtlHandler)
        .createRegistrySecret(kubernetesClient, namespace, imageDetails);

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPodCreateError() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    PodBuilder podBuilder = new PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildPodCreateErrorTaskParams();
    KubernetesConfig kubernetesConfig = cik8BuildTaskParams.getKubernetesConfig();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetails imageDetails =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetails();

    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptionDetails)).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doNothing().when(kubeCtlHandler).createRegistrySecret(kubernetesClient, namespace, imageDetails);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    doThrow(KubernetesClientException.class).when(kubeCtlHandler).createPod(kubernetesClient, podBuilder.build());

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithSuccess() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    PodBuilder podBuilder = new PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildPodCreateErrorTaskParams();
    KubernetesConfig kubernetesConfig = cik8BuildTaskParams.getKubernetesConfig();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetails imageDetails =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetails();

    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptionDetails)).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doNothing().when(kubeCtlHandler).createRegistrySecret(kubernetesClient, namespace, imageDetails);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(kubeCtlHandler.createPod(kubernetesClient, podBuilder.build())).thenReturn(podBuilder.build());
    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionResult.CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(CIK8BuildTaskHandler.Type.GCP_K8, cik8BuildTaskHandler.getType());
  }
}