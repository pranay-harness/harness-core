package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Provider;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ContainerResource;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Execable;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.TtyExecOutputErrorable;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.container.ImageDetails;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CIK8CtlHandlerTest extends WingsBaseTest {
  @Mock private SecretSpecBuilder mockSecretSpecBuilder;
  @Mock private KubernetesClient mockKubernetesClient;
  @Mock private MixedOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> mockKubeSecret;
  @Mock
  private NonNamespaceOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>>
      mockSecretNonNamespacedOp;
  @Mock private MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> mockKubePod;

  @InjectMocks private CIK8CtlHandler cik8CtlHandler;

  @Mock Provider<ExecCommandListener> execListenerProvider;
  @Mock private NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> mockPodNonNamespacedOp;
  @Mock private PodResource<Pod, DoneablePod> mockPodNamed;
  @Mock
  private ContainerResource<String, LogWatch, InputStream, PipedOutputStream, OutputStream, PipedInputStream, String,
      ExecWatch> mockContainerNamed;
  @Mock private TtyExecOutputErrorable<String, OutputStream, PipedInputStream, ExecWatch> mockRedirectedInput;
  @Mock private Execable<String, ExecWatch> mockExecable;
  @Mock private ExecWatch mockExecWatch;

  private static final String imageName = "IMAGE";
  private static final String tag = "TAG";
  private static final String podName = "pod";
  private static final String containerName = "container";
  private static final String namespace = "default";
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

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createRegistrySecretWithEmptyRegistry() {
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).build();
    when(mockSecretSpecBuilder.getRegistrySecretSpec(imageDetails, namespace)).thenReturn(null);
    cik8CtlHandler.createRegistrySecret(mockKubernetesClient, namespace, imageDetails);
    verify(mockSecretSpecBuilder).getRegistrySecretSpec(imageDetails, namespace);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createRegistrySecretWithRegistry() {
    ImageDetails imageDetails = ImageDetails.builder().name(imageName).tag(tag).build();
    Secret mockSecret = new SecretBuilder().build();
    Secret mockCreatedSecret = new SecretBuilder().build();
    when(mockSecretSpecBuilder.getRegistrySecretSpec(imageDetails, namespace)).thenReturn(mockSecret);
    when(mockKubernetesClient.secrets()).thenReturn(mockKubeSecret);
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockSecretNonNamespacedOp);
    when(mockSecretNonNamespacedOp.createOrReplace(mockSecret)).thenReturn(mockCreatedSecret);

    cik8CtlHandler.createRegistrySecret(mockKubernetesClient, namespace, imageDetails);
    verify(mockSecretSpecBuilder).getRegistrySecretSpec(imageDetails, namespace);
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
    when(mockKubePod.create(mockPod)).thenReturn(mockCreatedPod);

    assertEquals(mockCreatedPod, cik8CtlHandler.createPod(mockKubernetesClient, mockPod));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitSecretWithEmptyCreds() throws UnsupportedEncodingException {
    List<EncryptedDataDetail> gitEncryptedDataDetails = new ArrayList<>();
    GitConfig gitConfig = GitConfig.builder().build();
    when(mockSecretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace)).thenReturn(null);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConfig, gitEncryptedDataDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitSecretWithCreds() throws UnsupportedEncodingException {
    List<EncryptedDataDetail> gitEncryptedDataDetails = new ArrayList<>();
    GitConfig gitConfig = GitConfig.builder().build();
    Secret mockSecret = new SecretBuilder().build();
    Secret mockCreatedSecret = new SecretBuilder().build();
    when(mockSecretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace)).thenReturn(mockSecret);
    when(mockKubernetesClient.secrets()).thenReturn(mockKubeSecret);
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockSecretNonNamespacedOp);
    when(mockSecretNonNamespacedOp.createOrReplace(mockSecret)).thenReturn(mockCreatedSecret);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConfig, gitEncryptedDataDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
    verify(mockKubernetesClient).secrets();
    verify(mockKubeSecret).inNamespace(namespace);
    verify(mockSecretNonNamespacedOp).createOrReplace(mockSecret);
  }

  @Test(expected = UnsupportedEncodingException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitSecretWithException() throws UnsupportedEncodingException {
    List<EncryptedDataDetail> gitEncryptedDataDetails = new ArrayList<>();
    GitConfig gitConfig = GitConfig.builder().build();
    when(mockSecretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace))
        .thenThrow(UnsupportedEncodingException.class);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConfig, gitEncryptedDataDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
  }

  @Test(expected = TimeoutException.class)
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
    when(mockContainerNamed.redirectingInput()).thenReturn(mockRedirectedInput);
    when(mockRedirectedInput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.getReturnStatus(mockExecWatch, timeoutSecs)).thenThrow(TimeoutException.class);

    cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs);
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
    when(mockContainerNamed.redirectingInput()).thenReturn(mockRedirectedInput);
    when(mockRedirectedInput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.getReturnStatus(mockExecWatch, timeoutSecs)).thenThrow(InterruptedException.class);

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
    when(mockContainerNamed.redirectingInput()).thenReturn(mockRedirectedInput);
    when(mockRedirectedInput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.getReturnStatus(mockExecWatch, timeoutSecs)).thenReturn(false);

    assertFalse(cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs));
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
    when(mockContainerNamed.redirectingInput()).thenReturn(mockRedirectedInput);
    when(mockRedirectedInput.usingListener(execCommandListener)).thenReturn(mockExecable);
    when(mockExecable.exec(commands)).thenReturn(mockExecWatch);
    when(execCommandListener.getReturnStatus(mockExecWatch, timeoutSecs)).thenReturn(true);
    assertTrue(cik8CtlHandler.executeCommand(client, podName, containerName, namespace, commands, timeoutSecs));
  }
}