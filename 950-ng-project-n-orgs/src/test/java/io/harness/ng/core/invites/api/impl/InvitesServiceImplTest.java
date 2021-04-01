package io.harness.ng.core.invites.api.impl;

import static io.harness.ng.core.invites.InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
import static io.harness.ng.core.invites.InviteOperationResponse.FAIL;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_ADDED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_INVITED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.USER_INITIATED_INVITE;
import static io.harness.rule.OwnerRule.ANKUSH;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.account.remote.AccountClient;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.invites.InviteAcceptResponse;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.InvitesService;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.invites.ext.mail.EmailData;
import io.harness.ng.core.invites.ext.mail.MailUtils;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.repositories.invites.spring.InvitesRepository;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import com.auth0.jwt.interfaces.Claim;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.Response;

public class InvitesServiceImplTest extends CategoryTest {
  private static final String USER_VERIFICATION_SECRET = "abcde";
  private final String accountIdentifier = randomAlphabetic(7);
  private final String orgIdentifiier = randomAlphabetic(7);
  private final String projectIdentifier = randomAlphabetic(7);
  private final String emailId = String.format("%s@%s", randomAlphabetic(7), randomAlphabetic(7));
  private final String inviteId = randomAlphabetic(10);
  private final Role role = Role.builder().name("PROJECT ADMIN").build();
  @Mock private JWTGeneratorUtils jwtGeneratorUtils;
  @Mock private MailUtils mailUtils;
  @Mock private NgUserService ngUserService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private InvitesRepository invitesRepository;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) AccountClient accountClient;
  private Invite invite;

  private InvitesService invitesService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    MongoConfig mongoConfig = MongoConfig.builder().uri("mongodb://localhost:27017/ng-harness").build();
    invitesService = new InvitesServiceImpl(USER_VERIFICATION_SECRET, mongoConfig, jwtGeneratorUtils, mailUtils,
        ngUserService, transactionTemplate, invitesRepository, accountClient);
    invite = Invite.builder()
                 .accountIdentifier(accountIdentifier)
                 .orgIdentifier(orgIdentifiier)
                 .projectIdentifier(projectIdentifier)
                 .approved(Boolean.FALSE)
                 .email(emailId)
                 .name(randomAlphabetic(7))
                 .id(inviteId)
                 .role(role)
                 .inviteType(ADMIN_INITIATED_INVITE)
                 .build();
    when(accountClient.getAccountDTO(any()).execute())
        .thenReturn(Response.success(new RestResponse(AccountDTO.builder()
                                                          .identifier(accountIdentifier)
                                                          .companyName(accountIdentifier)
                                                          .name(accountIdentifier)
                                                          .build())));
    when(accountClient.getBaseUrl(any()).execute()).thenReturn(Response.success(new RestResponse("qa.harness.io")));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NullInvite() {
    InviteOperationResponse inviteOperationResponse = invitesService.create(null);
    assertThat(inviteOperationResponse).isEqualTo(InviteOperationResponse.FAIL);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserAlreadyAdded() {
    String userId = randomAlphabetic(10);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserProjectMap userProjectMap = UserProjectMap.builder()
                                        .userId(userId)
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifiier)
                                        .projectIdentifier(projectIdentifier)
                                        .roles(ImmutableList.of(role))
                                        .build();
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.of(userProjectMap));
    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);
    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_ADDED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserNotInvitedYet() {
    String userId = randomAlphabetic(10);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserProjectMap userProjectMap = UserProjectMap.builder()
                                        .userId(userId)
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifiier)
                                        .projectIdentifier(projectIdentifier)
                                        .roles(ImmutableList.of(Role.builder().name(randomAlphabetic(7)).build()))
                                        .build();
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.of(userProjectMap));
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITED_SUCCESSFULLY);
    verify(invitesRepository, times(1))
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedNot(
            any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserInvitedBefore() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    String userId = randomAlphabetic(10);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserProjectMap userProjectMap = UserProjectMap.builder()
                                        .userId(userId)
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifiier)
                                        .projectIdentifier(projectIdentifier)
                                        .roles(ImmutableList.of(Role.builder().name(randomAlphabetic(7)).build()))
                                        .build();
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.of(userProjectMap));
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_INVITED);
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(mailUtils, times(1)).sendMailAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_UserNotInvitedBefore() {
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITED_SUCCESSFULLY);
    verify(mailUtils, times(1)).sendMailAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_UserInvitedBefore() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_INVITED);
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(mailUtils, times(1)).sendMailAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_InviteAccepted() {
    invite.setApproved(Boolean.TRUE);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(ACCOUNT_INVITE_ACCEPTED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testResendInvitationMail_ValidInvite() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);

    when(invitesRepository.save(any())).thenReturn(invite);

    invitesService.resendInvitationMail(invite);

    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(mailUtils, times(1)).sendMailAsync(emailDataArgumentCaptor.capture());
    EmailData emailData = emailDataArgumentCaptor.getValue();
    assertThat(emailData.getAccountId()).isEqualTo(accountIdentifier);
    assertThat(emailData.getTo()).contains(emailId);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testUpdateInvite_ResendInvite() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<EmailData> emailDataArgumentCaptor = ArgumentCaptor.forClass(EmailData.class);

    when(invitesRepository.findFirstByIdAndDeletedNot(eq(inviteId), any())).thenReturn(Optional.of(invite));
    when(invitesRepository.save(any())).thenReturn(invite);

    invitesService.updateInvite(invite);

    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(mailUtils, times(1)).sendMailAsync(emailDataArgumentCaptor.capture());
    EmailData emailData = emailDataArgumentCaptor.getValue();
    assertThat(emailData.getAccountId()).isEqualTo(accountIdentifier);
    assertThat(emailData.getTo()).contains(emailId);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testUpdateInvite_ApproveRequest() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Update> updateArgumentCaptor = ArgumentCaptor.forClass(Update.class);
    invite.setApproved(Boolean.TRUE);
    invite.setInviteType(USER_INITIATED_INVITE);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(inviteId), any())).thenReturn(Optional.of(invite));

    Optional<Invite> inviteOptional = invitesService.updateInvite(invite);

    assertThat(inviteOptional.isPresent()).isTrue();
    Invite returnInvite = inviteOptional.get();
    assertThat(returnInvite.getApproved()).isEqualTo(Boolean.TRUE);
    assertThat(returnInvite.getRole()).isEqualTo(invite.getRole());
    assertThat(returnInvite.getId()).isEqualTo(invite.getId());
    verify(invitesRepository, atLeast(1)).updateInvite(idArgumentCaptor.capture(), updateArgumentCaptor.capture());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testUpdateInvite_ApproveRequest_UserExists() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Update> updateArgumentCaptor = ArgumentCaptor.forClass(Update.class);
    invite.setApproved(Boolean.TRUE);
    invite.setInviteType(USER_INITIATED_INVITE);
    String userId = randomAlphabetic(10);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(invite.getId()), any())).thenReturn(Optional.of(invite));

    Optional<Invite> inviteOptional = invitesService.updateInvite(invite);

    assertThat(inviteOptional.isPresent()).isTrue();
    Invite returnInvite = inviteOptional.get();
    assertThat(returnInvite.getApproved()).isEqualTo(Boolean.TRUE);
    assertThat(returnInvite.getRole()).isEqualTo(invite.getRole());
    assertThat(returnInvite.getId()).isEqualTo(invite.getId());
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), updateArgumentCaptor.capture());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(ngUserService, times(1)).createUserProjectMap(eq(invite), eq(user));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testDeleteInvite_InvitePresent() {
    ArgumentCaptor<Update> updateArgumentCaptor = ArgumentCaptor.forClass(Update.class);

    UpdateResult updateResult = mock(UpdateResult.class);
    when(updateResult.getModifiedCount()).thenReturn(1L);
    when(invitesRepository.save(any())).thenReturn(invite);
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(invite.getId()), any())).thenReturn(Optional.of(invite));
    when(invitesRepository.updateInvite(eq(inviteId), any())).thenReturn(updateResult);
    Optional<Invite> inviteOptional = invitesService.deleteInvite(inviteId);

    assertThat(inviteOptional.isPresent()).isTrue();
    Invite returnInvite = inviteOptional.get();
    assertThat(returnInvite.getId()).isEqualTo(invite.getId());
    verify(invitesRepository, times(1)).updateInvite(any(), updateArgumentCaptor.capture());
    Update update = updateArgumentCaptor.getValue();
    assertThat(update.getUpdateObject().size()).isEqualTo(1);
    assertThat(((Document) update.getUpdateObject().get("$set")).containsKey(InviteKeys.deleted)).isTrue();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testDeleteInvite_InvalidInvite() {
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(invite.getId()), any())).thenReturn(Optional.empty());

    Optional<Invite> inviteOptional = invitesService.deleteInvite(inviteId);

    assertThat(inviteOptional.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testVerify_ValidJWTToken_InviteIdDNE() {
    String jwtToken = randomAlphabetic(20);
    invite.setInviteToken(jwtToken);
    Claim idClaim = mock(Claim.class);
    String userId = randomAlphabetic(10);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(idClaim.asString()).thenReturn(inviteId + "1", inviteId);
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository.findFirstByIdAndDeletedNot(any(), any())).thenReturn(Optional.empty(), Optional.of(invite));
    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(ImmutableMap.of(InviteKeys.id, idClaim));

    InviteAcceptResponse operationResponse = invitesService.acceptInvite(jwtToken);
    assertThat(operationResponse.getResponse()).isEqualTo(FAIL);
  }
}
