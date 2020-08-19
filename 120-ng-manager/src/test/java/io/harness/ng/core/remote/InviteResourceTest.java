package io.harness.ng.core.remote;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.github.benas.randombeans.api.EnhancedRandom.randomListOf;
import static io.harness.rule.OwnerRule.ANKUSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.NGPageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.InvitesService;
import io.harness.ng.core.dto.CreateInviteListDTO;
import io.harness.ng.core.dto.InviteDTO;
import io.harness.ng.core.dto.RoleDTO;
import io.harness.ng.core.models.Invite;
import io.harness.ng.core.models.InviteType;
import io.harness.ng.core.models.Role;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.rule.Owner;
import io.harness.utils.PageTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import software.wings.service.impl.InviteOperationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InviteResourceTest extends CategoryTest {
  private InvitesService invitesService;
  private NgUserService ngUserService;
  private InviteResource inviteResource;
  private NGAccess ngAccess;

  @Before
  public void doSetup() {
    invitesService = mock(InvitesService.class);
    ngUserService = mock(NgUserService.class);
    inviteResource = new InviteResource(invitesService, ngUserService);

    ngAccess = getNGAccess();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testGetInvite() {
    String accountIdentifier = ngAccess.getAccountIdentifier();
    String orgIdentifier = ngAccess.getOrgIdentifier();
    String projectIdentifier = ngAccess.getProjectIdentifier();
    List<Invite> inviteList = new ArrayList<>();

    when(invitesService.list(any(), any())).thenReturn(PageTestUtils.getPage(inviteList, 0));

    NGPageResponse<InviteDTO> invitePage =
        inviteResource.get(accountIdentifier, orgIdentifier, projectIdentifier, 0, 10, new ArrayList<>()).getData();
    assertThat(invitePage.isEmpty()).isTrue();

    inviteList.add(createInvite(ngAccess));

    when(invitesService.list(any(), any())).thenReturn(PageTestUtils.getPage(inviteList, 1));

    invitePage =
        inviteResource.get(accountIdentifier, orgIdentifier, projectIdentifier, 0, 10, new ArrayList<>()).getData();

    assertThat(invitePage.isEmpty()).isFalse();
    assertThat(invitePage.getContent()).isNotNull();
    assertThat(invitePage.getContent().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreateInvitations() {
    int numUsers = 4;
    CreateInviteListDTO createInviteListDTO = createCreateInviteListDTO(numUsers);

    doReturn(InviteOperationResponse.USER_INVITED_SUCCESSFULLY).when(invitesService).create(any());
    doReturn(randomListOf(numUsers, String.class)).when(ngUserService).getUsernameFromEmail(any(), any());

    List<InviteOperationResponse> responses =
        inviteResource
            .createInvitations(ngAccess.getProjectIdentifier(), ngAccess.getOrgIdentifier(),
                ngAccess.getAccountIdentifier(), createInviteListDTO)
            .getData();

    ArgumentCaptor<Invite> inviteArgumentCaptor = ArgumentCaptor.forClass(Invite.class);

    verify(invitesService, times(numUsers)).create(inviteArgumentCaptor.capture());
    List<Invite> invites = inviteArgumentCaptor.getAllValues();
    List<String> emailIds = invites.stream().map(Invite::getEmail).collect(Collectors.toList());
    assertThat(emailIds).containsExactlyElementsOf(createInviteListDTO.getUsers());
  }

  private NGAccess getNGAccess() {
    return random(BaseNGAccess.class);
  }

  private String getEmail() {
    return random(String.class) + "@gmail.com";
  }

  private Invite createInvite(NGAccess ngAccess) {
    String email = getEmail();
    return Invite.builder()
        .name(random(String.class))
        .email(email)
        .role(random(Role.class))
        .inviteType(random(InviteType.class))
        .approved(random(boolean.class))
        .accountIdentifier(ngAccess.getAccountIdentifier())
        .orgIdentifier(ngAccess.getOrgIdentifier())
        .projectIdentifier(ngAccess.getProjectIdentifier())
        .build();
  }

  private CreateInviteListDTO createCreateInviteListDTO(int numUsers) {
    List<String> emailList = new ArrayList<>();
    for (int i = 0; i < numUsers; i++) {
      emailList.add(getEmail());
    }
    return CreateInviteListDTO.builder().users(emailList).role(random(RoleDTO.class)).build();
  }
}