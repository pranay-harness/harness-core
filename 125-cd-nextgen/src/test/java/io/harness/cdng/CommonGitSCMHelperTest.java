package io.harness.cdng;

import static io.harness.beans.FeatureName.OPTIMIZED_GIT_FETCH_FILES;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.TMACARI;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernamePasswordDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CommonGitSCMHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Spy @InjectMocks private CommonGitSCMHelper commonGitSCMHelper;
  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", "test-account").build();

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetGitStoreDelegateConfigFFEnabledGithubUsernameTokenAuth() {
    List<String> paths = asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .repoName(ParameterField.createValueField("parent-repo/module"))
                                        .paths(ParameterField.createValueField(paths))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    GithubConnectorDTO githubConnectorDTO =
        GithubConnectorDTO.builder()
            .connectionType(GitConnectionType.ACCOUNT)
            .url("http://localhost")
            .authentication(GithubAuthenticationDTO.builder()
                                .authType(GitAuthType.HTTP)
                                .credentials(GithubHttpCredentialsDTO.builder()
                                                 .type(GithubHttpAuthenticationType.USERNAME_AND_TOKEN)
                                                 .httpCredentialsSpec(GithubUsernameTokenDTO.builder()
                                                                          .username("usermane")
                                                                          .tokenRef(SecretRefData.builder().build())
                                                                          .build())
                                                 .build())
                                .build())
            .build();
    connectorInfoDTO.setConnectorConfig(githubConnectorDTO);

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), eq(OPTIMIZED_GIT_FETCH_FILES));
    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());

    GitStoreDelegateConfig gitStoreDelegateConfig = commonGitSCMHelper.getGitStoreDelegateConfig(
        gitStoreConfig, connectorInfoDTO, K8sManifestOutcome.builder().build(), paths, ambiance);

    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.isOptimizedFilesFetch()).isTrue();
    assertThat(gitStoreDelegateConfig.getGitConfigDTO()).isInstanceOf(GithubConnectorDTO.class);
    assertThat(gitStoreDelegateConfig.getApiAuthEncryptedDataDetails()).isEqualTo(apiEncryptedDataDetails);
    GithubConnectorDTO convertedGithubConnectorDTO = (GithubConnectorDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(convertedGithubConnectorDTO.getUrl()).isEqualTo("http://localhost/parent-repo/module");
    assertThat(convertedGithubConnectorDTO.getConnectionType()).isEqualTo(GitConnectionType.REPO);
    assertThat(convertedGithubConnectorDTO.getApiAccess()).isNotNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetGitStoreDelegateConfigFFEnabledGithubApiAccess() {
    List<String> paths = asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .repoName(ParameterField.createValueField("parent-repo/module"))
                                        .paths(ParameterField.createValueField(paths))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    GithubConnectorDTO githubConnectorDTO =
        GithubConnectorDTO.builder()
            .connectionType(GitConnectionType.ACCOUNT)
            .url("http://localhost")
            .apiAccess(GithubApiAccessDTO.builder().spec(GithubTokenSpecDTO.builder().build()).build())
            .authentication(GithubAuthenticationDTO.builder()
                                .authType(GitAuthType.HTTP)
                                .credentials(GithubHttpCredentialsDTO.builder()
                                                 .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                                                 .httpCredentialsSpec(GithubUsernamePasswordDTO.builder()
                                                                          .username("usermane")
                                                                          .passwordRef(SecretRefData.builder().build())
                                                                          .build())
                                                 .build())
                                .build())
            .build();
    connectorInfoDTO.setConnectorConfig(githubConnectorDTO);

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), eq(OPTIMIZED_GIT_FETCH_FILES));
    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());

    GitStoreDelegateConfig gitStoreDelegateConfig = commonGitSCMHelper.getGitStoreDelegateConfig(
        gitStoreConfig, connectorInfoDTO, K8sManifestOutcome.builder().build(), paths, ambiance);

    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.isOptimizedFilesFetch()).isTrue();
    assertThat(gitStoreDelegateConfig.getGitConfigDTO()).isInstanceOf(GithubConnectorDTO.class);
    assertThat(gitStoreDelegateConfig.getApiAuthEncryptedDataDetails()).isEqualTo(apiEncryptedDataDetails);
    GithubConnectorDTO convertedGithubConnectorDTO = (GithubConnectorDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(convertedGithubConnectorDTO.getUrl()).isEqualTo("http://localhost/parent-repo/module");
    assertThat(convertedGithubConnectorDTO.getConnectionType()).isEqualTo(GitConnectionType.REPO);
    assertThat(convertedGithubConnectorDTO.getApiAccess()).isNotNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetGitStoreDelegateConfigFFEnabledNotGithubOrGitlab() {
    List<String> paths = asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .repoName(ParameterField.createValueField("parent-repo/module"))
                                        .paths(ParameterField.createValueField(paths))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitConnectionType(GitConnectionType.ACCOUNT).url("http://localhost").build();
    connectorInfoDTO.setConnectorConfig(gitConfigDTO);

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), eq(OPTIMIZED_GIT_FETCH_FILES));
    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());

    GitStoreDelegateConfig gitStoreDelegateConfig = commonGitSCMHelper.getGitStoreDelegateConfig(
        gitStoreConfig, connectorInfoDTO, K8sManifestOutcome.builder().build(), paths, ambiance);

    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.getGitConfigDTO()).isInstanceOf(GitConfigDTO.class);
    assertThat(gitStoreDelegateConfig.isOptimizedFilesFetch()).isFalse();
    GitConfigDTO convertedConfig = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(convertedConfig.getUrl()).isEqualTo("http://localhost/parent-repo/module");
    assertThat(convertedConfig.getGitConnectionType()).isEqualTo(GitConnectionType.REPO);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetGitStoreDelegateConfigFFEnabledGithubUsernamePasswordAuth() {
    List<String> paths = asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .repoName(ParameterField.createValueField("parent-repo/module"))
                                        .paths(ParameterField.createValueField(paths))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    GithubConnectorDTO githubConnectorDTO =
        GithubConnectorDTO.builder()
            .connectionType(GitConnectionType.ACCOUNT)
            .url("http://localhost")
            .authentication(GithubAuthenticationDTO.builder()
                                .authType(GitAuthType.HTTP)
                                .credentials(GithubHttpCredentialsDTO.builder()
                                                 .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                                                 .httpCredentialsSpec(GithubUsernamePasswordDTO.builder()
                                                                          .username("usermane")
                                                                          .passwordRef(SecretRefData.builder().build())
                                                                          .build())
                                                 .build())
                                .build())
            .build();
    connectorInfoDTO.setConnectorConfig(githubConnectorDTO);

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), eq(OPTIMIZED_GIT_FETCH_FILES));
    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());

    GitStoreDelegateConfig gitStoreDelegateConfig = commonGitSCMHelper.getGitStoreDelegateConfig(
        gitStoreConfig, connectorInfoDTO, K8sManifestOutcome.builder().build(), paths, ambiance);

    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.getGitConfigDTO()).isInstanceOf(GitConfigDTO.class);
    assertThat(gitStoreDelegateConfig.isOptimizedFilesFetch()).isFalse();
    GitConfigDTO convertedConfig = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(convertedConfig.getUrl()).isEqualTo("http://localhost/parent-repo/module");
    assertThat(convertedConfig.getGitConnectionType()).isEqualTo(GitConnectionType.REPO);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void shouldGetGitStoreDelegateConfigOptimizedFilesFetchWithKustomize() {
    List<String> paths = asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
            .repoName(ParameterField.createValueField("parent-repo/module"))
            .paths(ParameterField.createValueField(paths))
            .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    GithubConnectorDTO githubConnectorDTO =
            GithubConnectorDTO.builder()
                    .connectionType(GitConnectionType.ACCOUNT)
                    .url("http://localhost")
                    .apiAccess(GithubApiAccessDTO.builder().spec(GithubTokenSpecDTO.builder().build()).build())
                    .authentication(GithubAuthenticationDTO.builder()
                            .authType(GitAuthType.HTTP)
                            .credentials(GithubHttpCredentialsDTO.builder()
                                    .type(GithubHttpAuthenticationType.USERNAME_AND_PASSWORD)
                                    .httpCredentialsSpec(GithubUsernamePasswordDTO.builder()
                                            .username("usermane")
                                            .passwordRef(SecretRefData.builder().build())
                                            .build())
                                    .build())
                            .build())
                    .build();
    connectorInfoDTO.setConnectorConfig(githubConnectorDTO);

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), eq(OPTIMIZED_GIT_FETCH_FILES));
    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());

    GitStoreDelegateConfig gitStoreDelegateConfig = commonGitSCMHelper.getGitStoreDelegateConfig(
            gitStoreConfig, connectorInfoDTO, KustomizeManifestOutcome.builder().build(), paths, ambiance);

    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.isOptimizedFilesFetch()).isFalse();
    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(gitConfigDTO.getUrl()).isEqualTo("http://localhost/parent-repo/module");
    assertThat(gitConfigDTO.getGitConnectionType()).isEqualTo(GitConnectionType.REPO);
  }
}
