package io.harness.ng.core.invites.remote;

import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_ADDED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_INVITED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.rule.OwnerRule.ANKUSH;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.api.InvitesService;
import io.harness.ng.core.invites.dto.CreateInviteListDTO;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.dto.RoleDTO;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;

public class InviteResourceTest extends CategoryTest {
  @Mock private InvitesService invitesService;
  @Mock private NgUserService ngUserService;

  private final String accountIdentifier = randomAlphabetic(7);
  private final String orgIdentifier = randomAlphabetic(7);
  private final String projectIdentifier = randomAlphabetic(7);
  private final String emailId = String.format("%s@%s", randomAlphabetic(7), randomAlphabetic(7));
  private final String inviteId = randomAlphabetic(10);
  private final Role role = Role.builder().name("PROJECT ADMIN").build();
  private Invite invite;

  private InviteResource inviteResource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    inviteResource = new InviteResource(invitesService, ngUserService);
    invite = Invite.builder()
                 .accountIdentifier(accountIdentifier)
                 .orgIdentifier(orgIdentifier)
                 .projectIdentifier(projectIdentifier)
                 .approved(Boolean.FALSE)
                 .email(emailId)
                 .name(randomAlphabetic(7))
                 .id(inviteId)
                 .role(role)
                 .inviteType(ADMIN_INITIATED_INVITE)
                 .build();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testGetInvites() {
    List<String> actualInviteIds = new ArrayList<>();
    List<Invite> inviteList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      inviteList.add(invite.withId(inviteId + i));
      actualInviteIds.add(inviteId + i);
    }
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).pageSize(10).sortOrders(null).build();
    when(invitesService.list(any(), any()))
        .thenReturn(new PageImpl<>(inviteList, PageUtils.getPageRequest(pageRequest), 10));

    List<InviteDTO> returnInvites =
        inviteResource.getInvites(accountIdentifier, orgIdentifier, projectIdentifier, pageRequest)
            .getData()
            .getContent();

    List<String> inviteIds = returnInvites.stream().map(InviteDTO::getId).collect(Collectors.toList());
    assertThat(inviteIds).isEqualTo(actualInviteIds);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreateInvitations() {
    List<String> emailIds = ImmutableList.of(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    CreateInviteListDTO createInviteListDTO = CreateInviteListDTO.builder()
                                                  .inviteType(ADMIN_INITIATED_INVITE)
                                                  .role(RoleDTO.builder().name("PROJECT ADMIN").build())
                                                  .users(emailIds)
                                                  .build();
    when(invitesService.create(any())).thenReturn(USER_ALREADY_INVITED, USER_INVITED_SUCCESSFULLY, USER_ALREADY_ADDED);
    List<InviteOperationResponse> operationResponses =
        inviteResource.createInvitations(accountIdentifier, orgIdentifier, projectIdentifier, createInviteListDTO)
            .getData();
    assertThat(operationResponses.get(0)).isEqualTo(USER_ALREADY_INVITED);
    assertThat(operationResponses.get(1)).isEqualTo(USER_INVITED_SUCCESSFULLY);
    assertThat(operationResponses.get(2)).isEqualTo(USER_ALREADY_ADDED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void updateInvite() {
    InviteDTO inviteDTO = InviteMapper.writeDTO(invite);
    when(invitesService.updateInvite(any())).thenReturn(Optional.of(invite), Optional.empty());

    Optional<InviteDTO> inviteOptional = inviteResource.updateInvite(inviteId, inviteDTO, accountIdentifier).getData();
    assertThat(inviteOptional.isPresent()).isTrue();

    inviteOptional = inviteResource.updateInvite(inviteId, inviteDTO, accountIdentifier).getData();
    assertThat(inviteOptional.isPresent()).isFalse();
  }
}
