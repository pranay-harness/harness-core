package io.harness.delegate.task.git;

import static io.harness.rule.OwnerRule.ABHINAV2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpec;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.task.shell.SshSessionConfigMapper;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class GitDecryptionHelperTest extends CategoryTest {
  @Mock private SecretDecryptionService decryptionService;
  @Mock private SshSessionConfigMapper sshSessionConfigMapper;
  @InjectMocks GitDecryptionHelper gitDecryptionHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testDecryptGitConfig() {
    gitDecryptionHelper.decryptGitConfig(GitConfig.builder().build(), new ArrayList<>());
    verify(decryptionService, times(1)).decrypt(any(GitConfig.class), anyListOf(EncryptedDataDetail.class));
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testSshSessionConfig() {
    SshSessionConfig expectedSshSessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();
    doReturn(expectedSshSessionConfig)
        .when(sshSessionConfigMapper)
        .getSSHSessionConfig(any(SSHKeySpecDTO.class), anyListOf(EncryptedDataDetail.class));
    SshSessionConfig actualSshSessionConfig =
        gitDecryptionHelper.getSSHSessionConfig(SSHKeySpecDTO.builder().build(), new ArrayList<>());
    assertThat(actualSshSessionConfig).isEqualTo(expectedSshSessionConfig);
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testSshSessionConfigGivenNullSpec() {
    SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(null, new ArrayList<>());
    assertThat(sshSessionConfig).isNull();
  }

  @Test
  @Owner(developers = ABHINAV2)
  @Category(UnitTests.class)
  public void testDecryptApiAccessConfig() {
    ScmConnector connector = GithubConnector.builder()
                                 .apiAccess(GithubApiAccess.builder()
                                                .type(GithubApiAccessType.GITHUB_APP)
                                                .spec(GithubAppSpec.builder().build())
                                                .build())
                                 .build();
    doReturn(connector)
        .when(decryptionService)
        .decrypt(any(DecryptableEntity.class), anyListOf(EncryptedDataDetail.class));

    gitDecryptionHelper.decryptApiAccessConfig(connector, new ArrayList<>());
    verify(decryptionService, times(1)).decrypt(any(DecryptableEntity.class), anyListOf(EncryptedDataDetail.class));
  }
}
