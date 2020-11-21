package io.harness.ng.core.invites.api.impl;

import static io.harness.ng.core.invites.InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_ADDED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITE_RESENT;
import static io.harness.ng.core.invites.entities.Invite.InviteKeys.deleted;
import static io.harness.ng.core.invites.entities.Invite.InviteKeys.id;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.ng.core.invites.entities.Invite.InviteType.USER_INITIATED_INVITE;
import static io.harness.rule.OwnerRule.ANKUSH;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.InvitesService;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Role;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.invites.ext.mail.EmailData;
import io.harness.ng.core.invites.ext.mail.MailUtils;
import io.harness.ng.core.invites.repositories.spring.InvitesRepository;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;

public class InvitesServiceImplTest extends CategoryTest {
  private final String BASE_URL = "baseurl";
  private final String USER_VERIFICATION_SECRET = "abcde";
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
  private Invite invite;

  private InvitesService invitesService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    MongoConfig mongoConfig = MongoConfig.builder().uri("mongodb://localhost:27017/ng-harness").build();
    invitesService = new InvitesServiceImpl(BASE_URL, USER_VERIFICATION_SECRET, mongoConfig, jwtGeneratorUtils,
        mailUtils, ngUserService, transactionTemplate, invitesRepository);
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
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserProjectMap userProjectMap = UserProjectMap.builder()
                                        .userId(userId)
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifiier)
                                        .projectIdentifier(projectIdentifier)
                                        .roles(ImmutableList.of(role))
                                        .build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.of(user));
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.of(userProjectMap));
    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);
    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_ADDED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserNotInvitedYet() {
    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserProjectMap userProjectMap = UserProjectMap.builder()
                                        .userId(userId)
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifiier)
                                        .projectIdentifier(projectIdentifier)
                                        .roles(ImmutableList.of(Role.builder().name(randomAlphabetic(7)).build()))
                                        .build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.of(user));
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
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserProjectMap userProjectMap = UserProjectMap.builder()
                                        .userId(userId)
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(orgIdentifiier)
                                        .projectIdentifier(projectIdentifier)
                                        .roles(ImmutableList.of(Role.builder().name(randomAlphabetic(7)).build()))
                                        .build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.of(userProjectMap));
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITE_RESENT);
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(mailUtils, times(1)).sendMailAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_UserNotInvitedBefore() {
    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.empty());
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

    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository
             .findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndRoleAndInviteTypeAndDeletedNot(
                 any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = invitesService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITE_RESENT);
    verify(invitesRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(mailUtils, times(1)).sendMailAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_InviteAccepted() {
    String userId = randomAlphabetic(10);
    invite.setApproved(Boolean.TRUE);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.empty());
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

    String userId = randomAlphabetic(10);
    when(invitesRepository.save(any())).thenReturn(invite);

    Invite returnInvite = invitesService.resendInvitationMail(invite);

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
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.empty());
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(inviteId), any())).thenReturn(Optional.of(invite));

    Optional<Invite> inviteOptional = invitesService.updateInvite(invite);

    assertTrue(inviteOptional.isPresent());
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
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(invite.getId()), any())).thenReturn(Optional.of(invite));

    Optional<Invite> inviteOptional = invitesService.updateInvite(invite);

    assertTrue(inviteOptional.isPresent());
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

    assertTrue(inviteOptional.isPresent());
    Invite returnInvite = inviteOptional.get();
    assertThat(returnInvite.getId()).isEqualTo(invite.getId());
    verify(invitesRepository, times(1)).updateInvite(any(), updateArgumentCaptor.capture());
    Update update = updateArgumentCaptor.getValue();
    assertThat(update.getUpdateObject().size()).isEqualTo(1);
    assertTrue(((Document) update.getUpdateObject().get("$set")).containsKey(deleted));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testDeleteInvite_InvalidInvite() {
    when(invitesRepository.findFirstByIdAndDeletedNot(eq(invite.getId()), any())).thenReturn(Optional.empty());

    Optional<Invite> inviteOptional = invitesService.deleteInvite(inviteId);

    assertFalse(inviteOptional.isPresent());
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testVerify_InvalidJWTToken() {
    Optional<Invite> inviteOptional = invitesService.verify(randomAlphabetic(20));
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testVerify_ValidJWTToken_InviteIdDNE() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Update> updateArgumentCaptor = ArgumentCaptor.forClass(Update.class);
    String jwtToken = randomAlphabetic(20);
    invite.setInviteToken(jwtToken);
    Claim idClaim = mock(Claim.class);
    String userId = randomAlphabetic(10);
    User user = User.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(idClaim.asString()).thenReturn(inviteId + "1", inviteId);
    when(ngUserService.getUserFromEmail(eq(accountIdentifier), eq(emailId))).thenReturn(Optional.of(user));
    when(invitesRepository.save(any())).thenReturn(invite);
    when(ngUserService.getUserProjectMap(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(invitesRepository.findFirstByIdAndDeletedNot(any(), any())).thenReturn(Optional.empty(), Optional.of(invite));
    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(ImmutableMap.of(id, idClaim));

    Optional<Invite> inviteOptional = invitesService.verify(jwtToken);
    assertFalse(inviteOptional.isPresent());

    inviteOptional = invitesService.verify(jwtToken);
    assertTrue(inviteOptional.isPresent());
    assertThat(inviteOptional.get().getId()).isEqualTo(inviteId);
  }
}
