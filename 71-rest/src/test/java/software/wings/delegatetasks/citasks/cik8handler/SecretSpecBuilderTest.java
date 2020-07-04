package software.wings.delegatetasks.citasks.cik8handler;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.delegatetasks.citasks.cik8handler.SecretSpecBuilder.SECRET_KEY;

import io.fabric8.kubernetes.api.model.Secret;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KmsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.container.ImageDetails;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretSpecBuilderTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;

  @InjectMocks private SecretSpecBuilder secretSpecBuilder;

  private static final String imageName = "IMAGE";
  private static final String tag = "TAG";
  private static final String namespace = "default";
  private static final String podName = "pod";
  private static final String containerName = "container";
  private static final String registryUrl = "https://index.docker.io/v1/";
  private static final String registrySecretName = "hs-index-docker-io-v1-usr-hs";
  private static final String userName = "usr";
  private static final String password = "pwd";
  private static final String gitRepoUrl = "https://github.com/wings-software/portal.git";
  private static final String gitSecretName = "hs-wings-software-portal-hs";
  private static final String sshSettingId = "setting-id";

  private GitConfig getGitConfigWithSshKeys() {
    HostConnectionAttributes hostConnectionAttributes =
        aHostConnectionAttributes()
            .withAccessType(KEY)
            .withAccountId(UUIDGenerator.generateUuid())
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey("Test Private Key".toCharArray())
            .build();
    SettingAttribute attr = SettingAttribute.Builder.aSettingAttribute().withValue(hostConnectionAttributes).build();
    return GitConfig.builder()
        .authenticationScheme(HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD)
        .sshSettingId(sshSettingId)
        .sshSettingAttribute(attr)
        .repoUrl(gitRepoUrl)
        .build();
  }
  private GitConfig getGitConfigWithSshKeysInvalidArg() {
    HostConnectionAttributes hostConnectionAttributes =
        aHostConnectionAttributes()
            .withAccessType(KEY)
            .withAccountId(UUIDGenerator.generateUuid())
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey("Test Private Key".toCharArray())
            .build();
    SettingValue value = AwsConfig.builder().build();
    SettingAttribute attr = SettingAttribute.Builder.aSettingAttribute().withValue(value).build();
    return GitConfig.builder()
        .authenticationScheme(HostConnectionAttributes.AuthenticationScheme.HTTP_PASSWORD)
        .sshSettingId(sshSettingId)
        .sshSettingAttribute(attr)
        .repoUrl(gitRepoUrl)
        .build();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldConvertCustomSecretVariables() throws IOException {
    Map<String, EncryptedDataDetail> encryptedVariables = new HashMap<>();

    EncryptedDataDetail encryptedDataDetail =
        EncryptedDataDetail.builder()
            .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
            .encryptionConfig(KmsConfig.builder()
                                  .accessKey("accessKey")
                                  .region("us-east-1")
                                  .secretKey("secretKey")
                                  .kmsArn("kmsArn")
                                  .build())
            .build();

    encryptedVariables.put("abc", encryptedDataDetail);
    when(encryptionService.getDecryptedValue(encryptedDataDetail)).thenReturn("pass".toCharArray());
    Map<String, String> decryptedSecrets = secretSpecBuilder.decryptCustomSecretVariables(encryptedVariables);
    assertThat(decryptedSecrets.get(SECRET_KEY + "abc")).isEqualTo(encodeBase64("pass"));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getRegistrySecretSpecWithEmptyCred() {
    ImageDetails imageDetails1 = ImageDetails.builder().name(imageName).tag(tag).build();
    ImageDetailsWithConnector imageDetailsWithConnector1 =
        ImageDetailsWithConnector.builder().imageDetails(imageDetails1).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetailsWithConnector1, namespace));

    ImageDetails imageDetails2 = ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).build();
    ImageDetailsWithConnector imageDetailsWithConnector2 =
        ImageDetailsWithConnector.builder().imageDetails(imageDetails2).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetailsWithConnector2, namespace));

    ImageDetails imageDetails3 =
        ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).username(userName).build();
    ImageDetailsWithConnector imageDetailsWithConnector3 =
        ImageDetailsWithConnector.builder().imageDetails(imageDetails3).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetailsWithConnector3, namespace));

    ImageDetails imageDetails4 =
        ImageDetails.builder().name(imageName).tag(tag).registryUrl(registryUrl).password(password).build();
    ImageDetailsWithConnector imageDetailsWithConnector4 =
        ImageDetailsWithConnector.builder().imageDetails(imageDetails4).build();
    assertNull(secretSpecBuilder.getRegistrySecretSpec(imageDetailsWithConnector4, namespace));
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getRegistrySecretSpecWithCred() {
    DockerConfig dockerConfig = DockerConfig.builder()
                                    .dockerRegistryUrl(registryUrl)
                                    .username(userName)
                                    .password(password.toCharArray())
                                    .build();

    List<EncryptedDataDetail> dockerConfigEncryptedDataDetails = mock(List.class);
    ImageDetails imageDetails1 = ImageDetails.builder()
                                     .name(imageName)
                                     .tag(tag)
                                     .registryUrl(registryUrl)
                                     .username(userName)
                                     .password(password)
                                     .build();

    when(encryptionService.decrypt(dockerConfig, dockerConfigEncryptedDataDetails)).thenReturn(dockerConfig);

    ImageDetailsWithConnector imageDetailsWithConnector1 = ImageDetailsWithConnector.builder()
                                                               .imageDetails(imageDetails1)
                                                               .encryptableSetting(dockerConfig)
                                                               .encryptedDataDetails(dockerConfigEncryptedDataDetails)
                                                               .build();

    Secret secret = secretSpecBuilder.getRegistrySecretSpec(imageDetailsWithConnector1, namespace);
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
        GitConfig.builder().authenticationScheme(HostConnectionAttributes.AuthenticationScheme.KERBEROS).build();
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

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getGitSecretSpecWithSshKeys() throws UnsupportedEncodingException {
    GitConfig gitConfig = getGitConfigWithSshKeys();
    List<EncryptedDataDetail> gitEncryptedDataDetails = mock(List.class);

    when(encryptionService.decrypt(gitConfig, gitEncryptedDataDetails)).thenReturn(null);
    gitConfig.setPassword(password.toCharArray());

    Secret secret = secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
    assertEquals(gitSecretName, secret.getMetadata().getName());
    assertEquals(namespace, secret.getMetadata().getNamespace());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getGitSecretSpecWithInvalidArgument() throws UnsupportedEncodingException {
    GitConfig gitConfig = getGitConfigWithSshKeysInvalidArg();
    List<EncryptedDataDetail> gitEncryptedDataDetails = mock(List.class);

    when(encryptionService.decrypt(gitConfig, gitEncryptedDataDetails)).thenReturn(null);
    gitConfig.setPassword(password.toCharArray());

    secretSpecBuilder.getGitSecretSpec(gitConfig, gitEncryptedDataDetails, namespace);
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDecryptDockerConfig() {
    Map<String, EncryptableSettingWithEncryptionDetails> map = new HashMap<>();
    EncryptableSettingWithEncryptionDetails setting =
        EncryptableSettingWithEncryptionDetails.builder()
            .encryptableSetting(DockerConfig.builder()
                                    .username("username")
                                    .password("password".toCharArray())
                                    .dockerRegistryUrl("https://index.docker.io/v1/")
                                    .build())
            .build();
    map.put("docker", setting);
    when(encryptionService.decrypt(any())).thenReturn(Collections.singletonList(setting));
    Map<String, String> data = secretSpecBuilder.decryptPublishArtifactSecretVariables(map);
    assertThat(data).containsKeys("USERNAME_docker", "PASSWORD_docker", "ENDPOINT_docker");
    assertThat(data.get("USERNAME_docker")).isEqualTo(encodeBase64("username"));
    assertThat(data.get("PASSWORD_docker")).isEqualTo(encodeBase64("password"));
    assertThat(data.get("ENDPOINT_docker")).isEqualTo(encodeBase64("https://index.docker.io/v1/"));
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDecryptAWSConfig() {
    Map<String, EncryptableSettingWithEncryptionDetails> map = new HashMap<>();
    EncryptableSettingWithEncryptionDetails setting =
        EncryptableSettingWithEncryptionDetails.builder()
            .encryptableSetting(
                AwsConfig.builder().accessKey("access-key").secretKey("secret-key".toCharArray()).build())
            .build();
    map.put("aws", setting);
    when(encryptionService.decrypt(any())).thenReturn(Collections.singletonList(setting));
    Map<String, String> data = secretSpecBuilder.decryptPublishArtifactSecretVariables(map);
    assertThat(data).containsKeys("ACCESS_KEY_aws", "SECRET_KEY_aws");
    assertThat(data.get("ACCESS_KEY_aws")).isEqualTo(encodeBase64("access-key"));
    assertThat(data.get("SECRET_KEY_aws")).isEqualTo(encodeBase64("secret-key"));
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDecryptArtifactoryConfig() {
    Map<String, EncryptableSettingWithEncryptionDetails> map = new HashMap<>();
    EncryptableSettingWithEncryptionDetails setting =
        EncryptableSettingWithEncryptionDetails.builder()
            .encryptableSetting(
                ArtifactoryConfig.builder().username("username").password("password".toCharArray()).build())
            .build();
    map.put("artifactory", setting);
    when(encryptionService.decrypt(any())).thenReturn(Collections.singletonList(setting));
    Map<String, String> data = secretSpecBuilder.decryptPublishArtifactSecretVariables(map);
    assertThat(data).containsKeys("USERNAME_artifactory", "PASSWORD_artifactory");
    assertThat(data.get("USERNAME_artifactory")).isEqualTo(encodeBase64("username"));
    assertThat(data.get("PASSWORD_artifactory")).isEqualTo(encodeBase64("password"));
  }

  @Test()
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCreateSecret() {
    Map<String, String> map = new HashMap<>();
    map.put("secret", "secret");
    Secret secret = secretSpecBuilder.createSecret("name", "namespace", map);
    assertThat(secret.getData()).isEqualTo(map);
    assertThat(secret.getMetadata().getName()).isEqualTo("name");
    assertThat(secret.getMetadata().getNamespace()).isEqualTo("namespace");
  }
}