package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.delegate.beans.ci.k8s.PodStatus.Status.ERROR;
import static io.harness.delegate.beans.ci.k8s.PodStatus.Status.RUNNING;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildGitSecretErrorTaskParams;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildImageSecretErrorTaskParams;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildPodCreateErrorTaskParams;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildTaskParams;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildTaskParamsWithPVC;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.buildTaskParamsWithPodSvc;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getCustomVarSecret;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getPublishArtifactSecrets;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getSecretVariableDetails;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.CIK8BuildTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.delegate.beans.ci.pod.CIK8ServicePodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.citasks.cik8handler.k8java.CIK8JavaClientHandler;
import io.harness.delegate.task.citasks.cik8handler.k8java.pod.PodSpecBuilder;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CIK8BuildTaskHandlerTest extends CategoryTest {
  @Mock private CIK8CtlHandler kubeCtlHandler;
  @Mock private PodSpecBuilder podSpecBuilder;
  @Mock private K8sConnectorHelper k8sConnectorHelper;
  @Mock private SecretSpecBuilder secretSpecBuilder;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  @Mock private K8EventHandler k8EventHandler;
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private CIK8JavaClientHandler cik8JavaClientHandler;
  @InjectMocks private CIK8BuildTaskHandler cik8BuildTaskHandler;

  private static final String namespace = "default";
  private static String storageClass = "test-storage";
  private static Integer storageMib = 100;
  private static String claimName = "pvc";
  private static String volume1 = "volume1";
  private static String secretName = "foo";
  private static final int timeout = 100;
  private static Secret imgSecret = new SecretBuilder()
                                        .withNewMetadata()
                                        .withName(secretName)
                                        .withNamespace(namespace)
                                        .endMetadata()
                                        .withData(ImmutableMap.of(".dockercfg", "test"))
                                        .build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithGitSecretError() {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    CIK8BuildTaskParams cik8BuildTaskParams = buildGitSecretErrorTaskParams();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    doThrow(UnsupportedEncodingException.class).when(secretSpecBuilder).decryptGitSecretVariables(any());

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithImageSecretError() {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);

    CIK8BuildTaskParams cik8BuildTaskParams = buildImageSecretErrorTaskParams();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    doThrow(KubernetesClientException.class)
        .when(kubeCtlHandler)
        .createRegistrySecret(eq(kubernetesClient), eq(namespace), any(), eq(imageDetailsWithConnector));

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPodCreateError() {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    V1PodBuilder podBuilder = new V1PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildPodCreateErrorTaskParams();

    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    when(kubeCtlHandler.createRegistrySecret(kubernetesClient, namespace, secretName, imageDetailsWithConnector))
        .thenReturn(imgSecret);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    doThrow(ApiException.class)
        .when(cik8JavaClientHandler)
        .createOrReplacePodWithRetries(kubernetesConfig, podBuilder.build(), namespace);

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPodReadyError() throws TimeoutException, InterruptedException, IOException {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    Watch<V1Event> watch = mock(Watch.class);
    V1PodBuilder podBuilder = new V1PodBuilder();

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParams();
    ConnectorDetails gitConnectorDetails = ConnectorDetails.builder().build();
    Map<String, SecretParams> gitSecretData = new HashMap<>();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    when(kubeCtlHandler.createRegistrySecret(kubernetesClient, namespace, secretName, imageDetailsWithConnector))
        .thenReturn(imgSecret);
    when(secretSpecBuilder.decryptGitSecretVariables(gitConnectorDetails)).thenReturn(gitSecretData);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(cik8JavaClientHandler.createOrReplacePodWithRetries(kubernetesConfig, podBuilder.build(), namespace))
        .thenReturn(podBuilder.build());
    when(k8EventHandler.startAsyncPodEventWatch(eq(kubernetesConfig), eq(namespace),
             eq(cik8BuildTaskParams.getCik8PodParams().getName()), eq(logStreamingTaskClient)))
        .thenReturn(watch);
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace, timeout))
        .thenReturn(PodStatus.builder().status(ERROR).build());
    doNothing().when(k8EventHandler).stopEventWatch(watch);

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithPVC() throws InterruptedException {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    V1PodBuilder podBuilder = new V1PodBuilder();
    Watch<V1Event> watch = mock(Watch.class);

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParamsWithPVC();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    when(kubeCtlHandler.createRegistrySecret(kubernetesClient, namespace, secretName, imageDetailsWithConnector))
        .thenReturn(imgSecret);
    doNothing().when(kubeCtlHandler).createPVC(kubernetesClient, namespace, claimName, storageClass, storageMib);
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(cik8JavaClientHandler.createOrReplacePodWithRetries(kubernetesConfig, podBuilder.build(), namespace))
        .thenReturn(podBuilder.build());
    when(k8EventHandler.startAsyncPodEventWatch(eq(kubernetesConfig), eq(namespace),
             eq(cik8BuildTaskParams.getCik8PodParams().getName()), eq(logStreamingTaskClient)))
        .thenReturn(watch);
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace, timeout))
        .thenReturn(PodStatus.builder().status(ERROR).build());
    doNothing().when(k8EventHandler).stopEventWatch(watch);

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithSuccess()
      throws UnsupportedEncodingException, TimeoutException, InterruptedException {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    V1PodBuilder podBuilder = new V1PodBuilder();
    Watch<V1Event> watch = mock(Watch.class);

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParams();
    Map<String, ConnectorDetails> publishArtifactEncryptedValues = cik8BuildTaskParams.getCik8PodParams()
                                                                       .getContainerParamsList()
                                                                       .get(0)
                                                                       .getContainerSecrets()
                                                                       .getConnectorDetailsMap();

    ConnectorDetails gitConnectorDetails = ConnectorDetails.builder().build();
    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    doNothing().when(kubeCtlHandler).createGitSecret(kubernetesClient, namespace, gitConnectorDetails);
    when(kubeCtlHandler.createRegistrySecret(kubernetesClient, namespace, secretName, imageDetailsWithConnector))
        .thenReturn(imgSecret);
    when(kubeCtlHandler.fetchCustomVariableSecretKeyMap(getSecretVariableDetails())).thenReturn(getCustomVarSecret());
    when(kubeCtlHandler.fetchConnectorsSecretKeyMap(publishArtifactEncryptedValues))
        .thenReturn(getPublishArtifactSecrets());
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(cik8JavaClientHandler.createOrReplacePodWithRetries(kubernetesConfig, podBuilder.build(), namespace))
        .thenReturn(podBuilder.build());
    when(k8EventHandler.startAsyncPodEventWatch(eq(kubernetesConfig), eq(namespace),
             eq(cik8BuildTaskParams.getCik8PodParams().getName()), eq(logStreamingTaskClient)))
        .thenReturn(watch);
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace, timeout))
        .thenReturn(PodStatus.builder().status(RUNNING).build());
    doNothing().when(k8EventHandler).stopEventWatch(watch);

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalWithServicePodSuccess() throws InterruptedException {
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    V1PodBuilder podBuilder = new V1PodBuilder();
    Watch<V1Event> watch = mock(Watch.class);

    CIK8BuildTaskParams cik8BuildTaskParams = buildTaskParamsWithPodSvc();
    Map<String, SecretParams> gitSecretData = new HashMap<>();
    Map<String, ConnectorDetails> publishArtifactConnectors = cik8BuildTaskParams.getCik8PodParams()
                                                                  .getContainerParamsList()
                                                                  .get(0)
                                                                  .getContainerSecrets()
                                                                  .getConnectorDetailsMap();

    ImageDetailsWithConnector imageDetailsWithConnector =
        cik8BuildTaskParams.getCik8PodParams().getContainerParamsList().get(0).getImageDetailsWithConnector();

    when(k8sConnectorHelper.getKubernetesConfig(any(ConnectorDetails.class))).thenReturn(kubernetesConfig);
    when(kubernetesHelperService.getKubernetesClient(kubernetesConfig)).thenReturn(kubernetesClient);
    when(secretSpecBuilder.decryptGitSecretVariables(cik8BuildTaskParams.getCik8PodParams().getGitConnector()))
        .thenReturn(gitSecretData);
    when(kubeCtlHandler.createRegistrySecret(kubernetesClient, namespace, secretName, imageDetailsWithConnector))
        .thenReturn(imgSecret);
    when(kubeCtlHandler.fetchCustomVariableSecretKeyMap(getSecretVariableDetails())).thenReturn(getCustomVarSecret());
    when(kubeCtlHandler.fetchConnectorsSecretKeyMap(publishArtifactConnectors)).thenReturn(getPublishArtifactSecrets());

    CIK8ServicePodParams servicePodParams = cik8BuildTaskParams.getServicePodParams().get(0);
    when(podSpecBuilder.createSpec((PodParams) servicePodParams.getCik8PodParams())).thenReturn(podBuilder);
    when(cik8JavaClientHandler.createOrReplacePodWithRetries(kubernetesConfig, podBuilder.build(), namespace))
        .thenReturn(podBuilder.build());
    doNothing().when(kubeCtlHandler).createService(eq(kubernetesClient), eq(namespace), any(), any(), any());
    when(podSpecBuilder.createSpec((PodParams) cik8BuildTaskParams.getCik8PodParams())).thenReturn(podBuilder);
    when(cik8JavaClientHandler.createOrReplacePodWithRetries(kubernetesConfig, podBuilder.build(), namespace))
        .thenReturn(podBuilder.build());
    when(k8EventHandler.startAsyncPodEventWatch(eq(kubernetesConfig), eq(namespace),
             eq(cik8BuildTaskParams.getCik8PodParams().getName()), eq(logStreamingTaskClient)))
        .thenReturn(watch);
    when(kubeCtlHandler.waitUntilPodIsReady(
             kubernetesClient, cik8BuildTaskParams.getCik8PodParams().getName(), namespace, timeout))
        .thenReturn(PodStatus.builder().status(RUNNING).build());
    doNothing().when(k8EventHandler).stopEventWatch(watch);

    K8sTaskExecutionResponse response =
        cik8BuildTaskHandler.executeTaskInternal(cik8BuildTaskParams, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getType() {
    assertEquals(CIK8BuildTaskHandler.Type.GCP_K8, cik8BuildTaskHandler.getType());
  }
}
