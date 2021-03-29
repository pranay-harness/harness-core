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
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketSshCredentialsDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.ng.userprofile.commons.BitbucketSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.entities.BitbucketSCM;
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
  public void testSave() {
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
    SourceCodeManager sourceCodeManager = bitbucketSCMCreate();
    List<SourceCodeManager> sourceCodeManagerList = new ArrayList<>(Arrays.asList(sourceCodeManager));
    when(sourceCodeManagerRepository.findByUserIdentifier(any())).thenReturn(sourceCodeManagerList);
    List<SourceCodeManagerDTO> sourceCodeManagerDTOList = sourceCodeManagerService.get();
    assertThat(sourceCodeManagerDTOList).hasSize(1);
    assertThat(sourceCodeManagerDTOList.get(0))
        .isEqualTo(scmMapBinder.get(sourceCodeManager.getType()).toSCMDTO(sourceCodeManager));
  }

  private SourceCodeManager bitbucketSCMCreate() {
    BitbucketAuthentication bitbucketAuthentication =
        BitbucketSshAuthentication.builder().sshKeyRef("ssh-key-ref").build();
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
}
