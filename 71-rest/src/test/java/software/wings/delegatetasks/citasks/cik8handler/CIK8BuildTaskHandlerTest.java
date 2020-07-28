package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildGitSecretErrorTaskParams;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildImageSecretErrorTaskParams;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildPodCreateErrorTaskParams;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildTaskParams;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildTaskParamsWithPVC;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getCustomVarSecret;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getEncryptedDetails;
import static software.wings.delegatetasks.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getPublishArtifactSecrets;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.PodParams;
import software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilder;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class CIK8BuildTaskHandlerTest extends WingsBaseTest {
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private CIK8CtlHandler kubeCtlHandler;
  @Mock private CIK8PodSpecBuilder podSpecBuilder;
  @Mock private EncryptionService encryptionService;
  @InjectMocks private CIK8BuildTaskHandler cik8BuildTaskHandler;

  private static final String namespace = "default";
  private static final String secretName = "secret";
  private static String storageClass = "test-storage";
  private static Integer storageMib = 100;
  private static String claimName = "pvc";
  private static String volume1 = "volume1";

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithGitSecretError() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8BuildTaskParams cik8BuildTaskParams = buildGitSecretErrorTaskParams();

    when(kubernetesHelperService.getKubernetesClient(any(KubernetesConfig.class))).thenReturn(kubernetesClient);
    doThrow(UnsupportedEncodingException.class)
        .when(kubeCtlHandler)
        .createGitSecret(kubernetesClient, namespace, cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig(),
            cik8BuildTaskParams.getGitFetchFilesConfig().getEncryptedDataDetails());

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithImageSecretError() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);

    CIK8BuildTaskParams cik8BuildTaskParams = buildImageSecretErrorTaskParams();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(kubernetesHelperService.getKubernetesClient(any(KubernetesConfig.class))).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doThrow(KubernetesClientException.class)
        .when(kubeCtlHandler)
        .createRegistrySecret(kubernetesClient, namespace, imageDetailsWithConnector);

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPodCreateError() throws UnsupportedEncodingException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    PodBuilder podBuilder = new PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildPodCreateErrorTaskParams();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(kubernetesHelperService.getKubernetesClient(any(KubernetesConfig.class))).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doNothing().when(kubeCtlHandler).createRegistrySecret(kubernetesClient, namespace, imageDetailsWithConnector);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    doThrow(KubernetesClientException.class)
        .when(kubeCtlHandler)
        .createPod(kubernetesClient, podBuilder.build(), namespace);

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPodReadyError()
      throws UnsupportedEncodingException, TimeoutException, InterruptedException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    PodBuilder podBuilder = new PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParams();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(kubernetesHelperService.getKubernetesClient(any(KubernetesConfig.class))).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doNothing().when(kubeCtlHandler).createRegistrySecret(kubernetesClient, namespace, imageDetailsWithConnector);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(kubeCtlHandler.createPod(kubernetesClient, podBuilder.build(), namespace)).thenReturn(podBuilder.build());
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace))
        .thenReturn(false);

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPVC() throws UnsupportedEncodingException, TimeoutException, InterruptedException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    PodBuilder podBuilder = new PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParamsWithPVC();
    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(kubernetesHelperService.getKubernetesClient(any(KubernetesConfig.class))).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createRegistrySecret(kubernetesClient, namespace, imageDetailsWithConnector);
    doNothing().when(kubeCtlHandler).createPVC(kubernetesClient, namespace, claimName, storageClass, storageMib);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(kubeCtlHandler.createPod(kubernetesClient, podBuilder.build(), namespace)).thenReturn(podBuilder.build());
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace))
        .thenReturn(false);

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithSuccess()
      throws UnsupportedEncodingException, TimeoutException, InterruptedException {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    PodBuilder podBuilder = new PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParams();
    Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues =
        cik8BuildTaskParams.getCik8PodParams()
            .getContainerParamsList()
            .get(0)
            .getContainerSecrets()
            .getPublishArtifactEncryptedValues();

    List<EncryptedDataDetail> encryptionDetails = cik8BuildTaskParams.getEncryptionDetails();
    GitConfig gitConfig = cik8BuildTaskParams.getGitFetchFilesConfig().getGitConfig();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    Secret secret = new SecretBuilder().withNewMetadata().withName(secretName).endMetadata().build();
    when(kubernetesHelperService.getKubernetesClient(any(KubernetesConfig.class))).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConfig, encryptionDetails);
    doNothing().when(kubeCtlHandler).createRegistrySecret(kubernetesClient, namespace, imageDetailsWithConnector);
    when(kubeCtlHandler.fetchCustomVariableSecretKeyMap(getEncryptedDetails())).thenReturn(getCustomVarSecret());
    when(kubeCtlHandler.fetchPublishArtifactSecretKeyMap(publishArtifactEncryptedValues))
        .thenReturn(getPublishArtifactSecrets());
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(kubeCtlHandler.createPod(kubernetesClient, podBuilder.build(), namespace)).thenReturn(podBuilder.build());
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace))
        .thenReturn(true);

    K8sTaskExecutionResponse response = cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(CIK8BuildTaskHandler.Type.GCP_K8, cik8BuildTaskHandler.getType());
  }
}