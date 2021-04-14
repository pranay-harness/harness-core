package io.harness.ng.core.invites.api;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.invites.InviteOperationResponse.ACCOUNT_INVITE_ACCEPTED;
import static io.harness.ng.core.invites.InviteOperationResponse.FAIL;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_ADDED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_ALREADY_INVITED;
import static io.harness.ng.core.invites.InviteOperationResponse.USER_INVITED_SUCCESSFULLY;
import static io.harness.ng.core.invites.entities.Invite.InviteType.ADMIN_INITIATED_INVITE;
import static io.harness.rule.OwnerRule.ANKUSH;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import io.harness.CategoryTest;
import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.invites.remote.InviteAcceptResponse;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.JWTGeneratorUtils;
import io.harness.ng.core.invites.api.impl.InviteServiceImpl;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.ng.core.invites.entities.Invite.InviteType;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.notification.NotificationResultWithStatus;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.repositories.invites.spring.InviteRepository;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import com.auth0.jwt.interfaces.Claim;
import com.mongodb.client.result.UpdateResult;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

@OwnedBy(PL)
public class InviteServiceImplTest extends CategoryTest {
  private static final String USER_VERIFICATION_SECRET = "abcde";
  private static final String accountIdentifier = randomAlphabetic(7);
  private static final String orgIdentifier = randomAlphabetic(7);
  private static final String projectIdentifier = randomAlphabetic(7);
  private static final String emailId = String.format("%s@%s", randomAlphabetic(7), randomAlphabetic(7));
  private static final String userId = randomAlphabetic(10);
  private static final String inviteId = randomAlphabetic(10);
  @Mock private JWTGeneratorUtils jwtGeneratorUtils;
  @Mock private NgUserService ngUserService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private InviteRepository inviteRepository;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) AccountClient accountClient;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private AccessControlAdminClient accessControlAdminClient;
  @Mock private NotificationClient notificationClient;

  private InviteService inviteService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    MongoConfig mongoConfig = MongoConfig.builder().uri("mongodb://localhost:27017/ng-harness").build();
    inviteService = new InviteServiceImpl(USER_VERIFICATION_SECRET, mongoConfig, jwtGeneratorUtils, ngUserService,
        transactionTemplate, inviteRepository, notificationClient, accessControlAdminClient, accountClient);

    when(accountClient.getAccountDTO(any()).execute())
        .thenReturn(Response.success(new RestResponse(AccountDTO.builder()
                                                          .identifier(accountIdentifier)
                                                          .companyName(accountIdentifier)
                                                          .name(accountIdentifier)
                                                          .build())));
    when(accountClient.getBaseUrl(any()).execute()).thenReturn(Response.success(new RestResponse("qa.harness.io")));
    when(notificationClient.sendNotificationAsync(any())).thenReturn(new NotificationResultWithStatus());
  }

  private Invite getDummyInvite() {
    return Invite.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .approved(Boolean.FALSE)
        .email(emailId)
        .name(randomAlphabetic(7))
        .id(inviteId)
        .roleBindings(getDummyRoleBinding())
        .inviteType(ADMIN_INITIATED_INVITE)
        .build();
  }

  private List<RoleBinding> getDummyRoleBinding() {
    return Collections.singletonList(RoleBinding.builder()
                                         .managedRole(false)
                                         .resourceGroupIdentifier(randomAlphabetic(7))
                                         .resourceGroupName(randomAlphabetic(7))
                                         .roleIdentifier(randomAlphabetic(7))
                                         .roleName(randomAlphabetic(7))
                                         .build());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NullInvite() {
    InviteOperationResponse inviteOperationResponse = inviteService.create(null);
    assertThat(inviteOperationResponse).isEqualTo(InviteOperationResponse.FAIL);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserAlreadyAdded() {
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    UserMembership userMembership = UserMembership.builder()
                                        .userId(userId)
                                        .emailId(emailId)
                                        .scopes(Collections.singletonList(UserMembership.Scope.builder()
                                                                              .accountIdentifier(accountIdentifier)
                                                                              .orgIdentifier(orgIdentifier)
                                                                              .projectIdentifier(projectIdentifier)
                                                                              .build()))
                                        .build();

    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(ngUserService.getUserMembership(any())).thenReturn(Optional.of(userMembership));
    InviteOperationResponse inviteOperationResponse = inviteService.create(getDummyInvite());
    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_ADDED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserAlreadyExists_UserNotInvitedYet() {
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user));
    when(ngUserService.getUserMembership(eq(userId))).thenReturn(Optional.empty());
    when(inviteRepository.save(any())).thenReturn(getDummyInvite());
    when(inviteRepository.findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InviteOperationResponse inviteOperationResponse = inviteService.create(getDummyInvite());

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITED_SUCCESSFULLY);
    verify(notificationClient, times(1)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserDNE_UserNotInvitedYet() {
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(inviteRepository.save(any())).thenReturn(getDummyInvite());
    when(inviteRepository.findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InviteOperationResponse inviteOperationResponse = inviteService.create(getDummyInvite());

    assertThat(inviteOperationResponse).isEqualTo(USER_INVITED_SUCCESSFULLY);
    verify(notificationClient, times(1)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_UserInvitedBefore() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();

    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.of(user), Optional.empty());
    when(ngUserService.getUserMembership(eq(userId))).thenReturn(Optional.empty());
    when(inviteRepository.save(any())).thenReturn(getDummyInvite());
    when(inviteRepository.findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(getDummyInvite()));

    //    when user exists
    InviteOperationResponse inviteOperationResponse = inviteService.create(getDummyInvite());

    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_INVITED);
    verify(inviteRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    String id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(notificationClient, times(1)).sendNotificationAsync(any());

    //    when user doesn't exists
    inviteOperationResponse = inviteService.create(getDummyInvite());

    assertThat(inviteOperationResponse).isEqualTo(USER_ALREADY_INVITED);
    verify(inviteRepository, atLeast(2)).updateInvite(idArgumentCaptor.capture(), any());
    id = idArgumentCaptor.getValue();
    assertThat(id).isEqualTo(inviteId);
    verify(notificationClient, times(2)).sendNotificationAsync(any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testCreate_NewUser_InviteAccepted() {
    Invite invite = Invite.builder()
                        .accountIdentifier(accountIdentifier)
                        .orgIdentifier(orgIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .approved(Boolean.FALSE)
                        .email(emailId)
                        .name(randomAlphabetic(7))
                        .id(inviteId)
                        .roleBindings(getDummyInvite().getRoleBindings())
                        .inviteType(ADMIN_INITIATED_INVITE)
                        .approved(Boolean.TRUE)
                        .build();
    when(ngUserService.getUserFromEmail(eq(emailId))).thenReturn(Optional.empty());
    when(inviteRepository.findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(invite));

    InviteOperationResponse inviteOperationResponse = inviteService.create(invite);

    assertThat(inviteOperationResponse).isEqualTo(ACCOUNT_INVITE_ACCEPTED);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void deleteInvite_inviteExists() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.of(getDummyInvite()));
    when(inviteRepository.updateInvite(any(), any())).thenReturn(UpdateResult.acknowledged(1, (long) 1, null));

    inviteService.deleteInvite(inviteId);

    verify(inviteRepository, times(1)).updateInvite(idArgumentCaptor.capture(), any());
    assertThat(idArgumentCaptor.getValue()).isEqualTo(inviteId);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void deleteInvite_InviteDNE() {
    ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.empty());

    inviteService.deleteInvite(inviteId);
    verify(inviteRepository, times(0)).updateInvite(idArgumentCaptor.capture(), any());
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void acceptInvite_InvalidJWTToken() {
    InviteAcceptResponse inviteAcceptResponse = inviteService.acceptInvite(null);
    assertThat(inviteAcceptResponse.getResponse()).isEqualTo(FAIL);

    inviteAcceptResponse = inviteService.acceptInvite("");
    assertThat(inviteAcceptResponse.getResponse()).isEqualTo(FAIL);

    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(Collections.emptyMap());

    inviteAcceptResponse = inviteService.acceptInvite("sadfs");
    assertThat(inviteAcceptResponse.getResponse()).isEqualTo(FAIL);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void acceptInvite_validToken() {
    String dummyJWTToken = "dummy invite token";
    Claim claim = mock(Claim.class);
    Invite invite = getDummyInvite();
    invite.setInviteToken(dummyJWTToken);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
    when(claim.asString()).thenReturn(inviteId);
    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(Collections.singletonMap(InviteKeys.id, claim));
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.of(invite));
    when(ngUserService.getUserFromEmail(any())).thenReturn(Optional.of(user));

    InviteAcceptResponse inviteAcceptResponse = inviteService.acceptInvite(dummyJWTToken);

    assertThat(inviteAcceptResponse.getResponse()).isEqualTo(ACCOUNT_INVITE_ACCEPTED);
    verify(inviteRepository, times(1)).updateInvite(idCapture.capture(), any());
    assertThat(idCapture.getValue()).isEqualTo(inviteId);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void updateInvite_invalidInviteId() {
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.empty());
    Optional<Invite> returnInvite = inviteService.updateInvite(getDummyInvite());
    assertThat(returnInvite.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void updateInvite_ValidInviteId() {
    String dummyJWTToken = "Dummy jwt token";
    Claim claim = mock(Claim.class);
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.of(getDummyInvite()));
    when(claim.asString()).thenReturn(inviteId);
    when(jwtGeneratorUtils.generateJWTToken(any(), any(), any())).thenReturn(dummyJWTToken);
    when(notificationClient.sendNotificationAsync(any())).thenReturn(NotificationResultWithStatus.builder().build());

    Optional<Invite> returnInvite = inviteService.updateInvite(getDummyInvite());
    assertThat(returnInvite.isPresent()).isTrue();
    assertThat(returnInvite.get().getInviteToken()).isEqualTo(dummyJWTToken);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void updateInvite_ValidInviteId_UserInitiatedInvite() {
    String dummyJWTToken = "Dummy jwt token";
    Invite invite = getDummyInvite();
    invite.setInviteType(InviteType.USER_INITIATED_INVITE);
    Claim claim = mock(Claim.class);
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.of(invite));
    when(claim.asString()).thenReturn(inviteId);
    when(jwtGeneratorUtils.generateJWTToken(any(), any(), any())).thenReturn(dummyJWTToken);
    when(notificationClient.sendNotificationAsync(any())).thenReturn(NotificationResultWithStatus.builder().build());

    assertThatThrownBy(() -> inviteService.updateInvite(invite)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void completeInvite_InvalidJWTToken() {
    boolean result = inviteService.completeInvite(null);
    assertThat(result).isFalse();

    result = inviteService.completeInvite("");
    assertThat(result).isFalse();

    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(Collections.emptyMap());

    result = inviteService.completeInvite("sadfs");
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void completeInvite_ValidToken_UserNotPresent() {
    String dummyJWTTOken = "dummy jwt token";
    Claim claim = mock(Claim.class);
    when(claim.asString()).thenReturn(inviteId);
    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(Collections.singletonMap(InviteKeys.id, claim));
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.of(getDummyInvite()));
    when(ngUserService.getUserFromEmail(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> inviteService.completeInvite(dummyJWTTOken)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void completeInvite_ValidToken() {
    String dummyJWTTOken = "dummy jwt token";
    Claim claim = mock(Claim.class);
    UserInfo user = UserInfo.builder().name(randomAlphabetic(7)).email(emailId).uuid(userId).build();
    ArgumentCaptor<Update> updateCapture = ArgumentCaptor.forClass(Update.class);
    ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
    when(claim.asString()).thenReturn(inviteId);
    when(jwtGeneratorUtils.verifyJWTToken(any(), any())).thenReturn(Collections.singletonMap(InviteKeys.id, claim));
    when(inviteRepository.findFirstByIdAndDeleted(any(), any())).thenReturn(Optional.of(getDummyInvite()));
    when(ngUserService.getUserFromEmail(any())).thenReturn(Optional.of(user));
    boolean result = inviteService.completeInvite(dummyJWTTOken);

    assertThat(result).isTrue();
    verify(accessControlAdminClient, times(1)).createMultiRoleAssignment(any(), any(), any(), any());
    verify(inviteRepository, times(1)).updateInvite(idCapture.capture(), updateCapture.capture());
    assertThat(idCapture.getValue()).isEqualTo(inviteId);
    assertThat(updateCapture.getValue().modifies(InviteKeys.deleted)).isTrue();
  }
}
