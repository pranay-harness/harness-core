package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CIK8CtlHandlerTest extends WingsBaseTest {
  @Mock private SecretSpecBuilder mockSecretSpecBuilder;
  @Mock private KubernetesClient mockKubernetesClient;
  @Mock private MixedOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>> mockKubeSecret;
  @Mock
  private NonNamespaceOperation<Secret, SecretList, DoneableSecret, Resource<Secret, DoneableSecret>>
      mockNonNamespacedOp;
  @Mock private MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> mockKubePod;

  @InjectMocks private CIK8CtlHandler cik8CtlHandler;

  private String imageName = "IMAGE";
  private String tag = "TAG";
  private String namespace = "default";

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
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockNonNamespacedOp);
    when(mockNonNamespacedOp.createOrReplace(mockSecret)).thenReturn(mockCreatedSecret);

    cik8CtlHandler.createRegistrySecret(mockKubernetesClient, namespace, imageDetails);
    verify(mockSecretSpecBuilder).getRegistrySecretSpec(imageDetails, namespace);
    verify(mockKubernetesClient).secrets();
    verify(mockKubeSecret).inNamespace(namespace);
    verify(mockNonNamespacedOp).createOrReplace(mockSecret);
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
    when(mockKubeSecret.inNamespace(namespace)).thenReturn(mockNonNamespacedOp);
    when(mockNonNamespacedOp.createOrReplace(mockSecret)).thenReturn(mockCreatedSecret);

    cik8CtlHandler.createGitSecret(mockKubernetesClient, namespace, gitConfig, gitEncryptedDataDetails);
    verify(mockSecretSpecBuilder).getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
    verify(mockKubernetesClient).secrets();
    verify(mockKubeSecret).inNamespace(namespace);
    verify(mockNonNamespacedOp).createOrReplace(mockSecret);
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
}