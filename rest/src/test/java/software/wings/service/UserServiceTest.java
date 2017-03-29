package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.EmailVerificationToken.Builder.anEmailVerificationToken;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.WORKFLOW;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_INVITE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.USER_PASSWORD;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserServiceTest extends WingsBaseTest {
  private final User.Builder userBuilder =
      anUser().withAppId(APP_ID).withEmail(USER_EMAIL).withName(USER_NAME).withPassword(PASSWORD);

  /**
   * The Update operations.
   */
  @Mock UpdateOperations<User> updateOperations;
  @Mock private EmailNotificationService<EmailData> emailDataNotificationService;
  @Mock private RoleService roleService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AccountService accountService;
  @Mock private AppService appService;
  @Inject @InjectMocks private UserService userService;
  @Captor private ArgumentCaptor<EmailData> emailDataArgumentCaptor;
  @Captor private ArgumentCaptor<User> userArgumentCaptor;
  @Captor private ArgumentCaptor<PageRequest<User>> pageRequestArgumentCaptor;
  @Mock Query<User> query;
  @Mock FieldEnd end;
  @Mock Query<EmailVerificationToken> verificationQuery;
  @Mock FieldEnd verificationQueryEnd;
  @Mock Query<UserInvite> userInviteQuery;
  @Mock FieldEnd userInviteQueryEnd;

  @Captor private ArgumentCaptor<UserInvite> userInviteCaptor;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(wingsPersistence.createQuery(User.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    when(updateOperations.add(any(), any())).thenReturn(updateOperations);

    when(wingsPersistence.createQuery(EmailVerificationToken.class)).thenReturn(verificationQuery);
    when(verificationQuery.field(any())).thenReturn(verificationQueryEnd);
    when(verificationQueryEnd.equal(any())).thenReturn(verificationQuery);

    when(wingsPersistence.createQuery(UserInvite.class)).thenReturn(userInviteQuery);
    when(userInviteQuery.field(any())).thenReturn(userInviteQueryEnd);
    when(userInviteQueryEnd.equal(any())).thenReturn(userInviteQueryEnd);
  }

  /**
   * Test register.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldRegisterNewUser() throws Exception {
    User savedUser = userBuilder.withUuid(USER_ID)
                         .withEmailVerified(false)
                         .withCompanyName(COMPANY_NAME)
                         .withAccountName(ACCOUNT_NAME)
                         .withPasswordHash(hashpw(PASSWORD, BCrypt.gensalt()))
                         .build();

    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(new EmailVerificationToken(USER_ID));
    when(accountService.save(any(Account.class)))
        .thenReturn(anAccount().withCompanyName(COMPANY_NAME).withUuid(ACCOUNT_ID).build());
    when(wingsPersistence.query(eq(User.class), any(PageRequest.class)))
        .thenReturn(PageResponse.Builder.aPageResponse().build());

    userService.register(userBuilder.build());

    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(BCrypt.checkpw(PASSWORD, userArgumentCaptor.getValue().getPasswordHash())).isTrue();
    assertThat(userArgumentCaptor.getValue().isEmailVerified()).isFalse();
    assertThat(userArgumentCaptor.getValue().getCompanyName()).isEqualTo(COMPANY_NAME);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(USER_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo("signup");
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("name")).isEqualTo(USER_NAME);
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url"))
        .startsWith(PORTAL_URL + "#" + VERIFICATION_PATH);
  }

  /**
   * Test register for existing user.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldRegisterExistingUser() throws Exception {
    User existingUser = userBuilder.withUuid(getUuid()).build();
    User savedUser = userBuilder.withUuid(USER_ID)
                         .withEmailVerified(false)
                         .withCompanyName(COMPANY_NAME)
                         .withAccountName(ACCOUNT_NAME)
                         .withPasswordHash(hashpw(PASSWORD, BCrypt.gensalt()))
                         .build();

    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(accountService.save(any(Account.class)))
        .thenReturn(anAccount().withCompanyName(COMPANY_NAME).withUuid(ACCOUNT_ID).build());
    when(wingsPersistence.query(eq(User.class), any(PageRequest.class)))
        .thenReturn(PageResponse.Builder.aPageResponse().withResponse(Lists.newArrayList(existingUser)).build());
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(anEmailVerificationToken().withToken("token123").build());

    userService.register(userBuilder.build());

    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(BCrypt.checkpw(PASSWORD, userArgumentCaptor.getValue().getPasswordHash())).isTrue();
    assertThat(userArgumentCaptor.getValue().isEmailVerified()).isFalse();
    assertThat(userArgumentCaptor.getValue().getCompanyName()).isEqualTo(COMPANY_NAME);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(USER_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo("signup");
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("name")).isEqualTo(USER_NAME);
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url"))
        .contains(VERIFICATION_PATH + "/token123");
  }

  /**
   * Should match password.
   */
  @Test
  public void shouldMatchPassword() {
    String hashpw = hashpw(PASSWORD, BCrypt.gensalt());
    assertThat(userService.matchPassword(PASSWORD, hashpw)).isTrue();
  }

  /**
   * Should update user.
   */
  @Test
  public void shouldUpdateUser() {
    List<Role> roles = Lists.newArrayList(
        aRole().withUuid(getUuid()).withRoleType(RoleType.APPLICATION_ADMIN).withAppId(getUuid()).build());
    User user =
        anUser().withAppId(APP_ID).withUuid(USER_ID).withEmail(USER_EMAIL).withName(USER_NAME).withRoles(roles).build();

    userService.update(user);
    verify(wingsPersistence).updateFields(User.class, USER_ID, ImmutableMap.of("name", USER_NAME, "roles", roles));
    verify(wingsPersistence).get(User.class, APP_ID, USER_ID);
  }

  /**
   * Should listStateMachines users.
   */
  @Test
  public void shouldListUsers() {
    PageRequest<User> request = new PageRequest<>();
    request.addFilter("appId", GLOBAL_APP_ID, EQ);
    userService.list(request);
    verify(wingsPersistence).query(eq(User.class), pageRequestArgumentCaptor.capture());
    SearchFilter filter = (SearchFilter) pageRequestArgumentCaptor.getValue().getFilters().get(0);
    assertThat(filter.getFieldName()).isEqualTo("appId");
    assertThat(filter.getFieldValues()).containsExactly(GLOBAL_APP_ID);
    assertThat(filter.getOp()).isEqualTo(EQ);
  }

  /**
   * Should delete user.
   */
  @Test
  public void shouldDeleteUser() {
    userService.delete(USER_ID);
    verify(wingsPersistence).delete(User.class, USER_ID);
  }

  /**
   * Should fetch user.
   */
  @Test
  public void shouldFetchUser() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    User user = userService.get(USER_ID);
    verify(wingsPersistence).get(User.class, USER_ID);
    assertThat(user).isEqualTo(userBuilder.withUuid(USER_ID).build());
  }

  /**
   * Should throw exception if user does not exist.
   */
  @Test
  public void shouldThrowExceptionIfUserDoesNotExist() {
    assertThatThrownBy(() -> userService.get("INVALID_USER_ID"))
        .isInstanceOf(WingsException.class)
        .hasMessage(USER_DOES_NOT_EXIST.getCode());
  }

  /**
   * Should verify email.
   */
  @Test
  public void shouldVerifyEmail() {
    when(verificationQuery.get())
        .thenReturn(anEmailVerificationToken().withUuid("TOKEN_ID").withUserId(USER_ID).withToken("TOKEN").build());

    userService.verifyEmail("TOKEN");

    verify(verificationQuery).field("appId");
    verify(verificationQueryEnd).equal(GLOBAL_APP_ID);
    verify(verificationQuery).field("token");
    verify(verificationQueryEnd).equal("TOKEN");
    verify(wingsPersistence).updateFields(User.class, USER_ID, ImmutableMap.of("emailVerified", true));
    verify(wingsPersistence).delete(EmailVerificationToken.class, "TOKEN_ID");
  }

  /**
   * Should send email.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       the io exception
   */
  @Test
  public void shouldSendEmail() throws EmailException, TemplateException, IOException {
    emailDataNotificationService.send(asList("anubhaw@gmail.com"), asList(), "wings-test", "hi");
  }

  /**
   * Test assign role to user.
   */
  @Test
  public void shouldAddRole() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(roleService.get(ROLE_ID)).thenReturn(aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());

    userService.addRole(USER_ID, ROLE_ID);
    verify(wingsPersistence, times(2)).get(User.class, USER_ID);
    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(USER_ID);
    verify(updateOperations).add("roles", aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());
  }

  /**
   * Test revoke role to user.
   */
  @Test
  public void shouldRevokeRole() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(roleService.get(ROLE_ID)).thenReturn(aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());

    userService.revokeRole(USER_ID, ROLE_ID);
    verify(wingsPersistence, times(2)).get(User.class, USER_ID);
    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(USER_ID);
    verify(updateOperations).removeAll("roles", aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());
  }

  @Test
  public void shouldCompleteInvite() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(accountService.get(ACCOUNT_ID)).thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).build());
    when(wingsPersistence.get(eq(UserInvite.class), any(PageRequest.class)))
        .thenReturn(anUserInvite().withUuid(USER_INVITE_ID).withAccountId(ACCOUNT_ID).withEmail(USER_EMAIL).build());

    UserInvite userInvite =
        anUserInvite().withAccountId(ACCOUNT_ID).withEmail(USER_EMAIL).withUuid(USER_INVITE_ID).build();
    userInvite.setName(USER_NAME);
    userInvite.setPassword(USER_PASSWORD);
    userService.completeInvite(userInvite);

    verify(wingsPersistence).save(userArgumentCaptor.capture());
    User savedUser = userArgumentCaptor.getValue();
    assertThat(BCrypt.checkpw(USER_PASSWORD, savedUser.getPasswordHash())).isTrue();
    assertThat(savedUser.isEmailVerified()).isTrue();
    assertThat(savedUser).hasFieldOrPropertyWithValue("email", USER_EMAIL);
  }

  @Test
  public void shouldGetAccountRole() {
    List<Role> roles =
        asList(aRole().withUuid(getUuid()).withRoleType(RoleType.ACCOUNT_ADMIN).withAccountId(ACCOUNT_ID).build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).withRoles(roles).build());
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build());

    AccountRole userAccountRole = userService.getUserAccountRole(USER_ID, ACCOUNT_ID);
    assertThat(userAccountRole)
        .isNotNull()
        .extracting("accountId", "accountName", "allApps")
        .containsExactly(ACCOUNT_ID, ACCOUNT_NAME, true);
    assertThat(userAccountRole.getResourceAccess()).isNotNull();
    for (ResourceType resourceType : ResourceType.values()) {
      for (Action action : Action.values()) {
        assertThat(userAccountRole.getResourceAccess()).contains(ImmutablePair.of(resourceType, action));
      }
    }
  }

  @Test
  public void shouldGetAccountForAllApsAdminRole() {
    List<Role> roles = asList(aRole()
                                  .withUuid(getUuid())
                                  .withRoleType(RoleType.APPLICATION_ADMIN)
                                  .withAccountId(ACCOUNT_ID)
                                  .withAllApps(true)
                                  .build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).withRoles(roles).build());
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build());

    AccountRole userAccountRole = userService.getUserAccountRole(USER_ID, ACCOUNT_ID);
    assertThat(userAccountRole)
        .isNotNull()
        .extracting("accountId", "accountName", "allApps")
        .containsExactly(ACCOUNT_ID, ACCOUNT_NAME, true);
    assertThat(userAccountRole.getResourceAccess()).isNotNull();
    for (ResourceType resourceType : Arrays.asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
      for (Action action : Action.values()) {
        assertThat(userAccountRole.getResourceAccess()).contains(ImmutablePair.of(resourceType, action));
      }
    }
  }

  @Test
  public void shouldGetApplicationRole() {
    List<Role> roles =
        asList(aRole().withUuid(getUuid()).withRoleType(RoleType.ACCOUNT_ADMIN).withAccountId(ACCOUNT_ID).build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).withRoles(roles).build());
    when(appService.get(APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).withAccountId(ACCOUNT_ID).build());

    ApplicationRole applicationRole = userService.getUserApplicationRole(USER_ID, APP_ID);
    assertThat(applicationRole)
        .isNotNull()
        .extracting("appId", "appName", "allEnvironments")
        .containsExactly(APP_ID, APP_NAME, true);
    assertThat(applicationRole.getResourceAccess()).isNotNull();
    for (ResourceType resourceType : Arrays.asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
      for (Action action : Action.values()) {
        assertThat(applicationRole.getResourceAccess()).contains(ImmutablePair.of(resourceType, action));
      }
    }
  }
}
