package io.harness.ng.userprofile.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.NgManagerTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketSshAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubAuthentication;
import io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabAuthentication;
import io.harness.connector.entities.embedded.gitlabconnector.GitlabSshAuthentication;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabSshCredentialsDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.ng.userprofile.commons.BitbucketSCMDTO;
import io.harness.ng.userprofile.commons.GithubSCMDTO;
import io.harness.ng.userprofile.commons.GitlabSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.entities.BitbucketSCM;
import io.harness.ng.userprofile.entities.GithubSCM;
import io.harness.ng.userprofile.entities.GitlabSCM;
import io.harness.ng.userprofile.entities.SourceCodeManager;
import io.harness.ng.userprofile.entities.SourceCodeManager.SourceCodeManagerMapper;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.repositories.ng.userprofile.spring.SourceCodeManagerRepository;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class SourceCodeManagerServiceImplTest extends NgManagerTestBase {
  SourceCodeManagerService sourceCodeManagerService;
  @Inject private Map<SCMType, SourceCodeManagerMapper> scmMapBinder;

  private SourceCodeManagerRepository sourceCodeManagerRepository;
  private String userIdentifier;
  private String name;
  private String sshKeyRef;

  @Before
  public void setup() {
    userIdentifier = generateUuid();
    sourceCodeManagerRepository = mock(SourceCodeManagerRepository.class);
    sourceCodeManagerService = new SourceCodeManagerServiceImpl(sourceCodeManagerRepository, scmMapBinder);
    Principal principal = mock(Principal.class);
    when(principal.getType()).thenReturn(PrincipalType.USER);
    when(principal.getName()).thenReturn(userIdentifier);
    SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    name = "some-name";
    sshKeyRef = "ssh-ref";
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testSaveBitbucket() {
    SourceCodeManagerDTO sourceCodeManagerDTO = bitbucketSCMDTOCreate();
    when(sourceCodeManagerRepository.save(any()))
        .thenReturn(scmMapBinder.get(sourceCodeManagerDTO.getType()).toSCMEntity(sourceCodeManagerDTO));
    SourceCodeManagerDTO savedSourceCodeManager = sourceCodeManagerService.save(sourceCodeManagerDTO);
    assertThat(savedSourceCodeManager).isEqualTo(sourceCodeManagerDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGet() {
    SourceCodeManager bitbucketSCM = bitbucketSCMCreate();
    SourceCodeManager githubSCM = githubSCMCreate();
    SourceCodeManager gitlabSCM = gitlabSCMCreate();
    List<SourceCodeManager> sourceCodeManagerList = new ArrayList<>(Arrays.asList(bitbucketSCM, githubSCM, gitlabSCM));
    when(sourceCodeManagerRepository.findByUserIdentifier(any())).thenReturn(sourceCodeManagerList);
    List<SourceCodeManagerDTO> sourceCodeManagerDTOList = sourceCodeManagerService.get();
    assertThat(sourceCodeManagerDTOList).hasSize(3);
    assertThat(sourceCodeManagerDTOList.get(0))
        .isEqualTo(scmMapBinder.get(bitbucketSCM.getType()).toSCMDTO(bitbucketSCM));
    assertThat(sourceCodeManagerDTOList.get(1)).isEqualTo(scmMapBinder.get(githubSCM.getType()).toSCMDTO(githubSCM));
    assertThat(sourceCodeManagerDTOList.get(2)).isEqualTo(scmMapBinder.get(gitlabSCM.getType()).toSCMDTO(gitlabSCM));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testSaveGithub() {
    SourceCodeManagerDTO sourceCodeManagerDTO = githubSCMDTOCreate();
    when(sourceCodeManagerRepository.save(any()))
        .thenReturn(scmMapBinder.get(sourceCodeManagerDTO.getType()).toSCMEntity(sourceCodeManagerDTO));
    SourceCodeManagerDTO savedSourceCodeManager = sourceCodeManagerService.save(sourceCodeManagerDTO);
    assertThat(savedSourceCodeManager).isEqualTo(sourceCodeManagerDTO);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testSaveGitlab() {
    SourceCodeManagerDTO sourceCodeManagerDTO = gitlabSCMDTOCreate();
    when(sourceCodeManagerRepository.save(any()))
        .thenReturn(scmMapBinder.get(sourceCodeManagerDTO.getType()).toSCMEntity(sourceCodeManagerDTO));
    SourceCodeManagerDTO savedSourceCodeManager = sourceCodeManagerService.save(sourceCodeManagerDTO);
    assertThat(savedSourceCodeManager).isEqualTo(sourceCodeManagerDTO);
  }

  private SourceCodeManager bitbucketSCMCreate() {
    BitbucketAuthentication bitbucketAuthentication = BitbucketSshAuthentication.builder().sshKeyRef(sshKeyRef).build();
    return BitbucketSCM.builder()
        .userIdentifier(userIdentifier)
        .name(name)
        .authType(GitAuthType.SSH)
        .authenticationDetails(bitbucketAuthentication)
        .build();
  }

  private SourceCodeManagerDTO bitbucketSCMDTOCreate() {
    BitbucketAuthenticationDTO bitbucketAuthenticationDTO =
        BitbucketAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                BitbucketSshCredentialsDTO.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();
    return BitbucketSCMDTO.builder()
        .userIdentifier(userIdentifier)
        .name(name)
        .bitbucketAuthenticationDTO(bitbucketAuthenticationDTO)
        .build();
  }

  private SourceCodeManager githubSCMCreate() {
    GithubAuthentication githubAuthentication = GithubSshAuthentication.builder().sshKeyRef(sshKeyRef).build();
    return GithubSCM.builder()
        .userIdentifier(userIdentifier)
        .name(name)
        .authType(GitAuthType.SSH)
        .authenticationDetails(githubAuthentication)
        .build();
  }

  private SourceCodeManagerDTO githubSCMDTOCreate() {
    GithubAuthenticationDTO githubAuthenticationDTO =
        GithubAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                GithubSshCredentialsDTO.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();
    return GithubSCMDTO.builder()
        .userIdentifier(userIdentifier)
        .name(name)
        .githubAuthenticationDTO(githubAuthenticationDTO)
        .build();
  }

  private SourceCodeManager gitlabSCMCreate() {
    GitlabAuthentication gitlabAuthentication = GitlabSshAuthentication.builder().sshKeyRef(sshKeyRef).build();
    return GitlabSCM.builder()
        .userIdentifier(userIdentifier)
        .name(name)
        .authType(GitAuthType.SSH)
        .authenticationDetails(gitlabAuthentication)
        .build();
  }

  private SourceCodeManagerDTO gitlabSCMDTOCreate() {
    GitlabAuthenticationDTO gitlabAuthenticationDTO =
        GitlabAuthenticationDTO.builder()
            .authType(GitAuthType.SSH)
            .credentials(
                GitlabSshCredentialsDTO.builder().sshKeyRef(SecretRefHelper.createSecretRef(sshKeyRef)).build())
            .build();
    return GitlabSCMDTO.builder()
        .userIdentifier(userIdentifier)
        .name(name)
        .authentication(gitlabAuthenticationDTO)
        .build();
  }
}
