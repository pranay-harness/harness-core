package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Secret;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.container.ImageDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SecretSpecBuilderTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;

  @InjectMocks private SecretSpecBuilder secretSpecBuilder;

  private static final String imageName = "IMAGE";
  private static final String tag = "TAG";
  private static final String namespace = "default";
  private static final String registryUrl = "https://index.docker.io/v1/";
  private static final String registrySecretName = "hs-index-docker-io-v1-usr-hs";
  private static final String userName = "usr";
  private static final String password = "pwd";
  private static final String gitRepoUrl = "https://github.com/wings-software/portal.git";
  private static final String gitSecretName = "hs-wings-software-portal-hs";

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getRegistrySecretSpecWithEmptyCred() {
    ImageDetails imageDetails1 = ImageDetails.builder().name(imageName).tag(tag).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetails1, namespace));

    ImageDetails imageDetails2 = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetails2, namespace));

    ImageDetails imageDetails3 =
        ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).username(userName).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetails3, namespace));

    ImageDetails imageDetails4 =
        ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).password(password).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetails4, namespace));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getRegistrySecretSpecWithCred() {
    ImageDetails imageDetails1 = ImageDetails.builder()
                                     .name(imageName)
                                     .tag(tag)
                                     .registryUrl(registryUrl)
                                     .username(userName)
                                     .password(password)
                                     .build();

    Secret secret = secretSpecBuilder.getRegistrySecretSpec(imageDetails1, namespace);
    assertEquals(registrySecretName, secret.getMetadata().getName());
    assertEquals(namespace, secret.getMetadata().getNamespace());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getGitSecretSpecWithEmptyCred() throws UnsupportedEncodingException {
    GitConfig gitConfig = null;
    List<EncryptedDataDetail> gitEncryptedDataDetails = new ArrayList<>();
    assertEquals(secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace), null);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getGitSecretSpecWithInvalidAuthSchme() throws UnsupportedEncodingException {
    GitConfig gitConfig =
        GitConfig.builder().authenticationScheme(HostConnectionAttributes.AuthenticationScheme.SSH_KEY).build();
    List<EncryptedDataDetail> gitEncryptedDataDetails = new ArrayList<>();

    secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getGitSecretSpecWithCred() throws UnsupportedEncodingException {
    GitConfig gitConfig = GitConfig.builder()
                              .authenticationScheme(HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD)
                              .username(userName)
                              .repoUrl(gitRepoUrl)
                              .build();
    List<EncryptedDataDetail> gitEncryptedDataDetails = mock(List.class);

    when(encryptionService.decrypt(gitConfig, gitEncryptedDataDetails)).thenReturn(null);
    gitConfig.setPassword(password.toCharArray());

    Secret secret = secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
    assertEquals(gitSecretName, secret.getMetadata().getName());
    assertEquals(namespace, secret.getMetadata().getNamespace());
  }
}