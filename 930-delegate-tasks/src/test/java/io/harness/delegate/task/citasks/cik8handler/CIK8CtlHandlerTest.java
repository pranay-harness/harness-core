package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.delegate.beans.ci.k8s.PodStatus.Status.RUNNING;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getCustomVarSecret;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getDockerSecret;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getPublishArtifactConnectorDetails;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getPublishArtifactSecrets;
import static io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandlerTestHelper.getSecretVariableDetails;
import static io.harness.delegate.task.citasks.cik8handler.params.CIConstants.POD_PENDING_PHASE;
import static io.harness.delegate.task.citasks.cik8handler.params.CIConstants.POD_RUNNING_PHASE;
import static io.harness.delegate.task.citasks.cik8handler.params.CIConstants.POD_WAIT_UNTIL_READY_SLEEP_SECS;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.exception.PodNotFoundException;
import io.harness.k8s.model.ImageDetails;
import io.harness.rule.Owner;
import io.harness.threading.Sleeper;

import com.google.inject.Provider;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStateWaitingBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.DoneablePersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ContainerResource;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Execable;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.TtyExecErrorable;
import io.fabric8.kubernetes.client.dsl.TtyExecOutputErrorable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CIK8CtlHandlerTest extends CategoryTest {
  @Mock private SecretSpecBuilder mockSecretSpecBuilder;
  @Mock private KubernetesClient mockKubernetesClient;
  @Mock private MixedOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> mockKubeSecret;
  @Mock
  private NonNamespaceOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>>
      mockSecretNonNamespacedOp;
  @Mock private MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> mockKubePod;
  @Mock private NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> mockPodNonNamespacedOp;
  @Mock private PodResource<Pod, DoneablePod> mockPodNamed;
  @Mock
  private MixedOperation<PersistentVolumeClaim, PersistentVolumeClaimList, DoneablePersistentVolumeClaim,
      Resource<PersistentVolumeClaim, DoneablePersistentVolumeClaim>> mockPVCOp;
  @Mock
  private NonNamespaceOperation<PersistentVolumeClaim, PersistentVolumeClaimList, DoneablePersistentVolumeClaim,
      Resource<PersistentVolumeClaim, DoneablePersistentVolumeClaim>> mockPVCNonNamespacedOp;

  @Mock private MixedOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>> mockSvcOp;
  @Mock
  private NonNamespaceOperation<Service, ServiceList, DoneableService, Resource<Service, DoneableService>>
      mockSvcNonNamespacedOp;
  @Mock Provider<ExecCommandListener> execListenerProvider;
  @Mock
  private ContainerResource<String, LogWatch, InputStream, PipedOutputStream, OutputStream, PipedInputStream, String,
      ExecWatch> mockContainerNamed;
  @Mock private TtyExecOutputErrorable<String, OutputStream, PipedInputStream, ExecWatch> mockRedirectedInput;
  @Mock private TtyExecErrorable<String, OutputStream, PipedInputStream, ExecWatch> mockWritingOutput;
  @Mock private Execable<String, ExecWatch> mockExecable;
  @Mock private ExecWatch mockExecWatch;

  @Mock private Sleeper sleeper;

  @InjectMocks private CIK8CtlHandler cik8CtlHandler;

  private static final String secretName = "imgsecret";
  private static final String imageName = "IMAGE";
  private static final String tag = "TAG";
  private static final String podName = "pod";
  private static final String containerName = "container";
  private static final String namespace = "default";
  private static final String storageClass = "default-storage";
  private static final Integer storageMib = 100;
  private static final String volumeName = "test-volume";

  private static final String[] commands = new String[] {"ls", "cd dir", "ls"};
  private static final String stdoutFilePath = "dir/stdout";
  private static final String stderrFilePath = "dir/stderr";
  private static final String[] dashCommandList =
      new String[] {"sh", "-c", "runCmd() { set -e; ls; cd dir; ls; }; runCmd > dir/stdout 2> dir/stderrCommands"};
  private static final String encodedDashCmd =
      "runCmd%28%29+%7B+set+-e%3B+ls%3B+cd+dir%3B+ls%3B+%7D%3B+runCmd+%3E+dir%2Fstdout+2%3E+dir%2Fstderr";
  private static final String dashShellStr = "sh";
  private static final String dashShellArg = "-c";
  private static final Integer timeoutSecs = 100;
  private static final int podWaitTimeoutSecs = 100;

  private Pod getPendingPod(String runningState) {
    PodStatus podStatus = new PodStatus();
    podStatus.setPhase(runningState);
    podStatus.setPodIP("123.12.11.11");
    podStatus.setContainerStatuses(Arrays.asList(getPendingContainerStatus()));
    return new PodBuilder().withStatus(podStatus).build();
  }

  private ContainerStatus getPendingContainerStatus() {
    ContainerStatus containerStatus = new ContainerStatus();
    ContainerStateWaiting containerStateWaiting = new ContainerStateWaitingBuilder().withMessage("pending").build();
    ContainerState containerState = new ContainerStateBuilder().withWaiting(containerStateWaiting).build();
    containerStatus.setState(containerState);
    return containerStatus;
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createRegistrySecretWithEmptyRegistry() {
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).build();
    ImageDetailsWithConnector imageDetailsWithConnector =
        ImageDetailsWithConnector.builder().imageDetails(imageDetails).build();
    when(mockSecretSpecBuilder.getRegistrySecretSpec(secretName, imageDetailsWithConnector, namespace))
        .thenReturn(null);
    cik8CtlHandler.createRegistrySecret(mockKubernetesClient, namespace, secretName, imageDetailsWithConnector);
    verify(mockSecretSpecBuilder).getRegistrySecretSpec(secretName, imageDetailsWithConnector, namespace);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createRegistrySecretWithRegistry() {
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).build();
    ImageDetailsWithConnector imageDetailsWithConnector =
        ImageDetailsWithConnector.builder().imageDetails(imageDetails).build();
    Secret mockSecret = new SecretBuilder().build();
    Secret mockCreatedSecret = new SecretBuilder().build();
    when(mockSecretSpecBuilder.getRegistrySecretSpec(secretName, imageDetailsWithConnector, namespace))
        .thenReturn(mockSecret);
    when(mockKubernetesClient.secrets()).thenReturn(mockKubeSecret);
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockSecretNonNamespacedOp);
    when(mockSecretNonNamespacedOp.createOrReplace(mockSecret)).thenReturn(mockCreatedSecret);

    cik8CtlHandler.createRegistrySecret(mockKubernetesClient, namespace, secretName, imageDetailsWithConnector);
    verify(mockSecretSpecBuilder).getRegistrySecretSpec(secretName, imageDetailsWithConnector, namespace);
    verify(mockKubernetesClient).secrets();
    verify(mockKubeSecret).inNamespace(namespace);
    verify(mockSecretNonNamespacedOp).createOrReplace(mockSecret);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createPod() {
    Pod mockPod = new PodBuilder().build();
    Pod mockCreatedPod = new PodBuilder().build();
    when(mockKubernetesClient.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.createOrReplace(mockPod)).thenReturn(mockCreatedPod);

    assertEquals(mockCreatedPod, cik8CtlHandler.createPod(mockKubernetesClient, mockPod, namespace));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void deletePodWithSuccess() {
    Pod mockPod = new PodBuilder().build();
    Pod mockCreatedPod = new PodBuilder().build();
    when(mockKubernetesClient.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.delete()).thenReturn(Boolean.TRUE);

    assertTrue(cik8CtlHandler.deletePod(mockKubernetesClient, podName, namespace));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void deletePodWithFailure() {
    Pod mockPod = new PodBuilder().build();
    Pod mockCreatedPod = new PodBuilder().build();
    when(mockKubernetesClient.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.delete()).thenReturn(Boolean.FALSE);

    assertFalse(cik8CtlHandler.deletePod(mockKubernetesClient, podName, namespace));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitSecretWithEmptyCreds() throws UnsupportedEncodingException {
    ConnectorDetails gitConnectorDetails = ConnectorDetails.builder().build();
    when(mockSecretSpecBuilder.getGitSecretSpec(gitConnectorDetails, namespace)).thenReturn(null);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConnectorDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConnectorDetails, namespace);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitSecretWithCreds() throws UnsupportedEncodingException {
    ConnectorDetails gitConnectorDetails = ConnectorDetails.builder().build();
    Secret mockSecret = new SecretBuilder().build();
    Secret mockCreatedSecret = new SecretBuilder().build();
    when(mockSecretSpecBuilder.getGitSecretSpec(gitConnectorDetails, namespace)).thenReturn(mockSecret);
    when(mockKubernetesClient.secrets()).thenReturn(mockKubeSecret);
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockSecretNonNamespacedOp);
    when(mockSecretNonNamespacedOp.createOrReplace(mockSecret)).thenReturn(mockCreatedSecret);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConnectorDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConnectorDetails, namespace);
    verify(mockKubernetesClient).secrets();
    verify(mockKubeSecret).inNamespace(namespace);
    verify(mockSecretNonNamespacedOp).createOrReplace(mockSecret);
  }

  @Test(expected = UnsupportedEncodingException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitSecretWithException() throws UnsupportedEncodingException {
    ConnectorDetails gitConnectorDetails = ConnectorDetails.builder().build();
    when(mockSecretSpecBuilder.getGitSecretSpec(gitConnectorDetails, namespace))
        .thenThrow(UnsupportedEncodingException.class);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConnectorDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConnectorDetails, namespace);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithTimeoutError() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    ExecCommandListener execCommandListener = mock(ExecCommandListener.class);

    when(execListenerProvider.get()).thenReturn(execCommandListener);
    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.inContainer(containerName)).thenReturn(mockContainerNamed);
    when(mockContainerNamed.writingOutput(any())).thenReturn(mockWritingOutput);
    when(mockWritingOutput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.isCommandExecutionComplete(timeoutSecs)).thenThrow(TimeoutException.class);
    doNothing().when(mockExecWatch).close();

    K8ExecCommandResponse response =
        cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs);
    assertEquals(ExecCommandStatus.TIMEOUT, response.getExecCommandStatus());
  }

  @Test(expected = InterruptedException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithInterruptedError() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    ExecCommandListener execCommandListener = mock(ExecCommandListener.class);

    when(execListenerProvider.get()).thenReturn(execCommandListener);
    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.inContainer(containerName)).thenReturn(mockContainerNamed);
    when(mockContainerNamed.writingOutput(any())).thenReturn(mockWritingOutput);
    when(mockWritingOutput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.isCommandExecutionComplete(timeoutSecs)).thenThrow(InterruptedException.class);
    doNothing().when(mockExecWatch).close();

    cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithFailure() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    ExecCommandListener execCommandListener = mock(ExecCommandListener.class);

    when(execListenerProvider.get()).thenReturn(execCommandListener);
    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.inContainer(containerName)).thenReturn(mockContainerNamed);
    when(mockContainerNamed.writingOutput(any())).thenReturn(mockWritingOutput);
    when(mockWritingOutput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.isCommandExecutionComplete(timeoutSecs)).thenReturn(false);
    doNothing().when(mockExecWatch).close();

    K8ExecCommandResponse response =
        cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs);
    assertEquals(ExecCommandStatus.ERROR, response.getExecCommandStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeCommandWithSuccess() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    ExecCommandListener execCommandListener = mock(ExecCommandListener.class);

    when(execListenerProvider.get()).thenReturn(execCommandListener);
    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.inContainer(containerName)).thenReturn(mockContainerNamed);
    when(mockContainerNamed.writingOutput(any())).thenReturn(mockWritingOutput);
    when(mockWritingOutput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.isCommandExecutionComplete(timeoutSecs)).thenReturn(true);
    doNothing().when(mockExecWatch).close();

    K8ExecCommandResponse response =
        cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs);
    assertEquals(ExecCommandStatus.SUCCESS, response.getExecCommandStatus());
  }

  @Test(expected = PodNotFoundException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void waitUntilPodIsReadyWithPodNotPresent() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);

    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.get()).thenThrow(new PodNotFoundException("not found"));

    cik8CtlHandler.waitUntilPodIsReady(client, podName, namespace, podWaitTimeoutSecs);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void waitUntilPodIsReadyWithSuccess() throws TimeoutException, InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    Pod pod = new PodBuilder()
                  .withStatus(new PodStatusBuilder().withPodIP("123.12.11.11").withPhase(POD_RUNNING_PHASE).build())
                  .build();

    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.get()).thenReturn(pod);

    assertEquals(
        RUNNING, cik8CtlHandler.waitUntilPodIsReady(client, podName, namespace, podWaitTimeoutSecs).getStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void waitUntilPodIsReadyWithSuccessAfterOneRetry() throws InterruptedException {
    KubernetesClient client = mock(KubernetesClient.class);
    Pod pod1 = getPendingPod(POD_PENDING_PHASE);
    Pod pod2 = new PodBuilder()
                   .withStatus(new PodStatusBuilder().withPodIP("123.12.11.11").withPhase(POD_RUNNING_PHASE).build())
                   .build();

    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.get()).thenReturn(pod1);
    doNothing().when(sleeper).sleep(POD_WAIT_UNTIL_READY_SLEEP_SECS * 1000);

    when(client.pods()).thenReturn(mockKubePod);
    when(mockKubePod.inNamespace(namespace)).thenReturn(mockPodNonNamespacedOp);
    when(mockPodNonNamespacedOp.withName(podName)).thenReturn(mockPodNamed);
    when(mockPodNamed.get()).thenReturn(pod2);

    assertEquals(
        RUNNING, cik8CtlHandler.waitUntilPodIsReady(client, podName, namespace, podWaitTimeoutSecs).getStatus());
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFetchCustomVariableSecretKeyMap() {
    List<SecretVariableDetails> secretVariableDetails = getSecretVariableDetails();
    when(mockSecretSpecBuilder.decryptCustomSecretVariables(secretVariableDetails)).thenReturn(getCustomVarSecret());
    Map<String, SecretParams> secretKeyMap = cik8CtlHandler.fetchCustomVariableSecretKeyMap(secretVariableDetails);
    assertThat(secretKeyMap).isEqualTo(getCustomVarSecret());
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFetchPublishArtifactSecretKeyMap() {
    Map<String, ConnectorDetails> publishArtifactSettings = getPublishArtifactConnectorDetails();
    when(mockSecretSpecBuilder.decryptConnectorSecretVariables(publishArtifactSettings))
        .thenReturn(getPublishArtifactSecrets());

    Map<String, SecretParams> secretParams = cik8CtlHandler.fetchConnectorsSecretKeyMap(publishArtifactSettings);
    assertThat(secretParams).isEqualTo(getPublishArtifactSecrets());
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCreateSecret() {
    Map<String, SecretParams> dockerSecret = getDockerSecret();
    Map<String, String> decryptedSecret =
        dockerSecret.values().stream().collect(Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
    Secret secret = new SecretBuilder()
                        .withNewMetadata()
                        .withName("secret-name")
                        .withNamespace(namespace)
                        .endMetadata()
                        .withType("opaque")
                        .withData(decryptedSecret)
                        .build();
    when(mockSecretSpecBuilder.createSecret("secret-name", namespace, decryptedSecret)).thenReturn(secret);
    when(mockKubernetesClient.secrets()).thenReturn(mockKubeSecret);
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockSecretNonNamespacedOp);
    when(mockSecretNonNamespacedOp.createOrReplace(secret)).thenReturn(secret);
    Secret secret1 = cik8CtlHandler.createSecret(mockKubernetesClient, "secret-name", namespace, decryptedSecret);
    assertThat(secret1).isEqualTo(secret);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createPVC() {
    PersistentVolumeClaim pvc = mock(PersistentVolumeClaim.class);
    when(mockKubernetesClient.persistentVolumeClaims()).thenReturn(mockPVCOp);
    when(mockPVCOp.inNamespace(namespace)).thenReturn(mockPVCNonNamespacedOp);
    when(mockPVCNonNamespacedOp.create(any())).thenReturn(pvc);
    cik8CtlHandler.createPVC(mockKubernetesClient, namespace, volumeName, storageClass, storageMib);
    verify(mockKubernetesClient).persistentVolumeClaims();
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createService() {
    Service svc = mock(Service.class);
    String serviceName = "svc";
    List<Integer> ports = new ArrayList<>();
    ports.add(8000);
    Map<String, String> selector = new HashMap<>();
    selector.put("foo", "bar");
    when(mockKubernetesClient.services()).thenReturn(mockSvcOp);
    when(mockSvcOp.inNamespace(namespace)).thenReturn(mockSvcNonNamespacedOp);
    when(mockSvcNonNamespacedOp.create(any())).thenReturn(svc);
    cik8CtlHandler.createService(mockKubernetesClient, namespace, serviceName, selector, ports);
    verify(mockKubernetesClient).services();
  }
}
