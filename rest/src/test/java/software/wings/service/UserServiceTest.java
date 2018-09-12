package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.EmailVerificationToken.Builder.anEmailVerificationToken;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.WORKFLOW;
import static software.wings.service.impl.UserServiceImpl.INVITE_EMAIL_TEMPLATE_NAME;
import static software.wings.service.impl.UserServiceImpl.SIGNUP_EMAIL_TEMPLATE_NAME;
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
import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import freemarker.template.TemplateException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.mail.EmailException;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;

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
  /**
   * The Query.
   */
  @Mock Query<User> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  /**
   * The Verification query.
   */
  @Mock Query<EmailVerificationToken> verificationQuery;
  /**
   * The Verification query end.
   */

  /**
   * The User invite query.
   */
  @Mock Query<UserInvite> userInviteQuery;
  /**
   * The User invite query end.
   */

  @Mock private EmailNotificationService emailDataNotificationService;
  @Mock private RoleService roleService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AccountService accountService;
  @Mock private AppService appService;
  @Mock private AuthService authService;
  @Mock private CacheHelper cacheHelper;
  /**
   * The Cache.
   */
  @Mock Cache<String, User> cache;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;
  @Inject @InjectMocks private UserService userService;
  @Captor private ArgumentCaptor<EmailData> emailDataArgumentCaptor;
  @Captor private ArgumentCaptor<User> userArgumentCaptor;
  @Captor private ArgumentCaptor<PageRequest<User>> pageRequestArgumentCaptor;
  @Captor private ArgumentCaptor<UserInvite> userInviteCaptor;
  @Inject @InjectMocks SecretManager secretManager;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(cacheHelper.getUserCache()).thenReturn(cache);

    when(wingsPersistence.createQuery(User.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);

    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    when(updateOperations.addToSet(any(), any())).thenReturn(updateOperations);
    when(updateOperations.set(any(), any())).thenReturn(updateOperations);
    when(updateOperations.addToSet(any(), any())).thenReturn(updateOperations);

    when(wingsPersistence.createQuery(EmailVerificationToken.class)).thenReturn(verificationQuery);
    when(verificationQuery.filter(any(), any())).thenReturn(verificationQuery);

    when(wingsPersistence.createQuery(UserInvite.class)).thenReturn(userInviteQuery);
    when(userInviteQuery.filter(any(), any())).thenReturn(userInviteQuery);
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
                         .withPasswordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                         .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(configuration.getPortal().getVerificationUrl()).thenReturn(VERIFICATION_PATH);
    when(configuration.getPortal().getAllowedDomainsList().isEmpty()).thenReturn(true);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(new EmailVerificationToken(USER_ID));
    when(accountService.save(any(Account.class)))
        .thenReturn(anAccount().withCompanyName(COMPANY_NAME).withUuid(ACCOUNT_ID).build());
    when(wingsPersistence.query(eq(User.class), any(PageRequest.class))).thenReturn(aPageResponse().build());

    userService.register(userBuilder.build());

    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(BCrypt.checkpw(new String(PASSWORD), userArgumentCaptor.getValue().getPasswordHash())).isTrue();
    assertThat(userArgumentCaptor.getValue().isEmailVerified()).isFalse();
    assertThat(userArgumentCaptor.getValue().getCompanyName()).isEqualTo(COMPANY_NAME);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(USER_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo(SIGNUP_EMAIL_TEMPLATE_NAME);
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("name")).isEqualTo(USER_NAME);
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url"))
        .startsWith(PORTAL_URL + "/#" + VERIFICATION_PATH);
    verify(cache).remove(USER_ID);
  }

  /**
   * Test register for existing user.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldRegisterExistingUser() throws Exception {
    User existingUser = userBuilder.withUuid(generateUuid()).build();
    User savedUser = userBuilder.withUuid(USER_ID)
                         .withEmailVerified(false)
                         .withCompanyName(COMPANY_NAME)
                         .withAccountName(ACCOUNT_NAME)
                         .withPasswordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                         .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(configuration.getPortal().getVerificationUrl()).thenReturn(VERIFICATION_PATH);
    when(configuration.getPortal().getAllowedDomainsList().isEmpty()).thenReturn(true);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(accountService.save(any(Account.class)))
        .thenReturn(anAccount().withCompanyName(COMPANY_NAME).withUuid(ACCOUNT_ID).build());
    when(wingsPersistence.query(eq(User.class), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(Lists.newArrayList(existingUser)).build());
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(anEmailVerificationToken().withToken("token123").build());

    userService.register(userBuilder.build());

    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(BCrypt.checkpw(new String(PASSWORD), userArgumentCaptor.getValue().getPasswordHash())).isTrue();
    assertThat(userArgumentCaptor.getValue().isEmailVerified()).isFalse();
    assertThat(userArgumentCaptor.getValue().getCompanyName()).isEqualTo(COMPANY_NAME);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(USER_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo("signup");
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("name")).isEqualTo(USER_NAME);
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url"))
        .contains(VERIFICATION_PATH + "/token123");
    verify(cache).remove(USER_ID);
  }

  /**
   * Should match password.
   */
  @Test
  public void shouldMatchPassword() {
    String hashpw = hashpw(new String(PASSWORD), BCrypt.gensalt());
    assertThat(userService.matchPassword(PASSWORD, hashpw)).isTrue();
  }

  /**
   * Should update user.
   */
  @Test
  public void shouldUpdateUser() {
    List<Role> roles = Lists.newArrayList(
        aRole().withUuid(generateUuid()).withRoleType(RoleType.APPLICATION_ADMIN).withAppId(generateUuid()).build());
    User user =
        anUser().withAppId(APP_ID).withUuid(USER_ID).withEmail(USER_EMAIL).withName(USER_NAME).withRoles(roles).build();
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set("name", USER_NAME);
    updateOperations.set("roles", roles);

    userService.update(user);
    verify(wingsPersistence).update(user, updateOperations);
    verify(wingsPersistence).get(User.class, APP_ID, USER_ID);
    verify(cache).remove(USER_ID);
  }

  /**
   * Should update user profile.
   */
  @Test
  public void shouldUpdateUserProfile() {
    User user = anUser().withAppId(APP_ID).withUuid(USER_ID).withEmail(USER_EMAIL).withName("test").build();
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set("name", "test");

    userService.update(user);
    verify(wingsPersistence).update(user, updateOperations);
    verify(wingsPersistence).get(User.class, APP_ID, USER_ID);
    assertThat(user.getName().equals("test"));
    verify(cache).remove(USER_ID);
  }

  /**
   * Should list users.
   */
  @Test
  public void shouldListUsers() {
    PageRequest<User> request = new PageRequest<>();
    request.addFilter("appId", EQ, GLOBAL_APP_ID);
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
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(wingsPersistence.delete(User.class, USER_ID)).thenReturn(true);
    userService.delete(ACCOUNT_ID, USER_ID);
    verify(wingsPersistence).delete(User.class, USER_ID);
    verify(cache).remove(USER_ID);
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
        .hasMessage(USER_DOES_NOT_EXIST.name());
  }

  /**
   * Should verify email.
   */
  @Test
  public void shouldVerifyEmail() {
    when(verificationQuery.get())
        .thenReturn(anEmailVerificationToken().withUuid("TOKEN_ID").withUserId(USER_ID).withToken("TOKEN").build());

    userService.verifyToken("TOKEN");

    verify(verificationQuery).filter("appId", GLOBAL_APP_ID);
    verify(verificationQuery).filter("token", "TOKEN");
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
    emailDataNotificationService.send(
        EmailData.builder().to(asList("anubhaw@gmail.com")).subject("wings-test").body("hi").build());
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
    verify(query).filter(Mapper.ID_KEY, USER_ID);
    verify(updateOperations).addToSet("roles", aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());
    verify(cache).remove(USER_ID);
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
    verify(query).filter(Mapper.ID_KEY, USER_ID);
    verify(updateOperations).removeAll("roles", aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());
    verify(cache).remove(USER_ID);
  }

  /**
   * Should invite new user.
   */
  @Test
  public void shouldInviteNewUser() throws EmailException, TemplateException, IOException {
    UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                .withAppId(GLOBAL_APP_ID)
                                .withAccountId(ACCOUNT_ID)
                                .withEmails(asList(USER_EMAIL))
                                .withRoles(asList(aRole().withUuid(ROLE_ID).build()))
                                .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount()
                        .withCompanyName(COMPANY_NAME)
                        .withUuid(ACCOUNT_ID)
                        .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                        .build());
    when(wingsPersistence.save(userInvite)).thenReturn(USER_INVITE_ID);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class)))
        .thenReturn(userBuilder.withUuid(USER_ID).build());

    userService.inviteUsers(userInvite);
    verify(accountService).get(ACCOUNT_ID);
    verify(wingsPersistence).save(userInvite);
    verify(wingsPersistence).get(UserInvite.class, GLOBAL_APP_ID, USER_INVITE_ID);
    verify(wingsPersistence).saveAndGet(eq(User.class), any(User.class));
    verify(cache).remove(USER_ID);

    // verify the outgoing email template
    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo(INVITE_EMAIL_TEMPLATE_NAME);

    User sameUser = new User();
    sameUser.setEmail(USER_EMAIL);
    sameUser.setName(USER_EMAIL);
    when(wingsPersistence.createQuery(User.class).get()).thenReturn(sameUser);
    // mock out addToSet
    when(updateOperations.addToSet(anyString(), any(Account.class))).thenReturn(updateOperations);

    // now try to invite same user again, should still be "signup" and not "role"
    userService.inviteUsers(userInvite);
    verify(emailDataNotificationService, times(2)).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo(INVITE_EMAIL_TEMPLATE_NAME);
  }

  /**
   * Should invite new user - mixed case email.
   */
  @Test
  public void shouldInviteNewUserMixedCaseEmail() throws EmailException, TemplateException, IOException {
    String mixedEmail = "UseR@wings.software ";
    UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                .withAppId(GLOBAL_APP_ID)
                                .withAccountId(ACCOUNT_ID)
                                .withEmails(asList(mixedEmail))
                                .withRoles(asList(aRole().withUuid(ROLE_ID).build()))
                                .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount()
                        .withCompanyName(COMPANY_NAME)
                        .withUuid(ACCOUNT_ID)
                        .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                        .build());
    when(wingsPersistence.save(userInvite)).thenReturn(USER_INVITE_ID);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class)))
        .thenReturn(userBuilder.withUuid(USER_ID).build());

    userService.inviteUsers(userInvite);

    verify(accountService).get(ACCOUNT_ID);
    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(userArgumentCaptor.getValue()).hasFieldOrPropertyWithValue("email", mixedEmail.trim().toLowerCase());
  }

  /**
   * Should invite existing user.
   */
  @Test
  @Ignore
  public void shouldInviteExistingUser() {
    UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                .withAppId(GLOBAL_APP_ID)
                                .withAccountId(ACCOUNT_ID)
                                .withEmails(asList(USER_EMAIL))
                                .withRoles(asList(aRole().withUuid(ROLE_ID).build()))
                                .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withCompanyName(COMPANY_NAME).withUuid(ACCOUNT_ID).build());
    when(query.get()).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(wingsPersistence.save(userInvite)).thenReturn(USER_INVITE_ID);

    userService.inviteUsers(userInvite);

    verify(query, times(2)).field("email");
    verify(end, times(2)).equal(USER_EMAIL);
    verify(accountService).get(ACCOUNT_ID);
    verify(wingsPersistence).save(userInvite);
    verify(wingsPersistence).get(UserInvite.class, GLOBAL_APP_ID, USER_INVITE_ID);
  }

  /**
   * Should Override 2FA on user
   */
  @Test
  @Ignore
  public void shouldOverrideTwoFactorForUser() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(wingsPersistence.get(Account.class, ACCOUNT_ID))
        .thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).build());
    // userService.overrideTwoFactorforAccount(ACCOUNT_ID, true);
  }

  /**
   * Should complete invite.
   */
  @Test
  public void shouldCompleteInvite() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(accountService.get(ACCOUNT_ID)).thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).build());
    when(wingsPersistence.get(eq(UserInvite.class), any(PageRequest.class)))
        .thenReturn(anUserInvite().withUuid(USER_INVITE_ID).withAccountId(ACCOUNT_ID).withEmail(USER_EMAIL).build());
    when(query.get()).thenReturn(userBuilder.withUuid(USER_ID).build());

    UserInvite userInvite =
        anUserInvite().withAccountId(ACCOUNT_ID).withEmail(USER_EMAIL).withUuid(USER_INVITE_ID).build();
    userInvite.setName(USER_NAME);
    userInvite.setPassword(USER_PASSWORD);
    userService.completeInvite(userInvite);

    verify(wingsPersistence).updateFields(eq(User.class), eq(USER_ID), any(HashMap.class));
  }

  /**
   * Should get account role.
   */
  @Test
  public void shouldGetAccountRole() {
    List<Role> roles =
        asList(aRole().withUuid(generateUuid()).withRoleType(RoleType.ACCOUNT_ADMIN).withAccountId(ACCOUNT_ID).build());
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

  /**
   * Should get account for all aps admin role.
   */
  @Test
  public void shouldGetAccountForAllApsAdminRole() {
    List<Role> roles = asList(aRole()
                                  .withUuid(generateUuid())
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
    for (ResourceType resourceType : asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
      for (Action action : Action.values()) {
        assertThat(userAccountRole.getResourceAccess()).contains(ImmutablePair.of(resourceType, action));
      }
    }
  }

  /**
   * Should get application role.
   */
  @Test
  public void shouldGetApplicationRole() {
    List<Role> roles =
        asList(aRole().withUuid(generateUuid()).withRoleType(RoleType.ACCOUNT_ADMIN).withAccountId(ACCOUNT_ID).build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).withRoles(roles).build());
    when(appService.get(APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).withAccountId(ACCOUNT_ID).build());

    ApplicationRole applicationRole = userService.getUserApplicationRole(USER_ID, APP_ID);
    assertThat(applicationRole)
        .isNotNull()
        .extracting("appId", "appName", "allEnvironments")
        .containsExactly(APP_ID, APP_NAME, true);
    assertThat(applicationRole.getResourceAccess()).isNotNull();
    for (ResourceType resourceType : asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
      for (Action action : Action.values()) {
        assertThat(applicationRole.getResourceAccess()).contains(ImmutablePair.of(resourceType, action));
      }
    }
  }

  /**
   * Should send reset password email.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       the io exception
   */
  @Test
  public void shouldSendResetPasswordEmail() throws EmailException, TemplateException, IOException {
    ArrayList<Account> accounts = new ArrayList<>();
    accounts.add(new Account());
    when(query.get()).thenReturn(userBuilder.withUuid(USER_ID).withAccounts(accounts).build());
    when(configuration.getPortal().getJwtPasswordSecret()).thenReturn("SECRET");
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    userService.resetPassword(USER_EMAIL);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(USER_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo("reset_password");
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("name")).isEqualTo(USER_NAME);
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url"))
        .startsWith(PORTAL_URL + "/#/reset-password/");
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url").length())
        .isGreaterThan((PORTAL_URL + "/#/reset-password/").length());
  }

  /**
   * Should update password.
   *
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  @Test
  public void shouldUpdatePassword() throws UnsupportedEncodingException {
    when(query.get()).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(configuration.getPortal().getJwtPasswordSecret()).thenReturn("SECRET");
    Algorithm algorithm = Algorithm.HMAC256("SECRET");
    String token = JWT.create()
                       .withIssuer("Harness Inc")
                       .withIssuedAt(new Date())
                       .withExpiresAt(new Date(System.currentTimeMillis() + 4 * 60 * 60 * 1000)) // 4 hrs
                       .withClaim("email", USER_EMAIL)
                       .sign(algorithm);

    userService.updatePassword(token, USER_PASSWORD);

    verify(query).filter("email", USER_EMAIL);
    verify(authService).invalidateAllTokensForUser(USER_ID);
    verify(wingsPersistence).update(eq(userBuilder.withUuid(USER_ID).build()), any(UpdateOperations.class));
    verify(updateOperations).set(eq("passwordHash"), anyString());
    verify(updateOperations).set(eq("passwordChangedAt"), anyLong());
  }

  /**
   * Should add Account.
   *
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  @Test
  @Ignore
  public void shouldAddAccount() throws UnsupportedEncodingException {
    Account account =
        anAccount().withUuid(ACCOUNT_ID).withCompanyName(COMPANY_NAME).withAccountName(ACCOUNT_NAME).build();
    when(accountService.save(any(Account.class))).thenReturn(account);
    when(roleService.getAccountAdminRole(ACCOUNT_ID)).thenReturn(aRole().build());
    when(updateOperations.addToSet(any(), any())).thenReturn(updateOperations);
    User user = anUser().withUuid(USER_ID).withEmail(USER_EMAIL).build();
    Account created = userService.addAccount(account, user);
    assertThat(created).isEqualTo(account);
    verify(accountService).exists(eq(ACCOUNT_NAME));
  }

  @Test
  public void testJWTToken() {
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getJwtMultiAuthSecret())
        .thenReturn("5E1YekVGldTSS5Kt0GHlyWrJ6fJHmee9nXSBssefAWSOgdMwAvvbvJalnYENZ0H0EealN0CxHh34gUCN");
    assertThat(
        userService.verifyJWTToken(userService.generateJWTToken("testUser@harness.io", JWT_CATEGORY.MULTIFACTOR_AUTH),
            JWT_CATEGORY.MULTIFACTOR_AUTH))
        .isEqualTo(null);

    try {
      userService.verifyJWTToken(
          userService.generateJWTToken("testUser@harness.io", JWT_CATEGORY.MULTIFACTOR_AUTH) + "fakeData",
          JWT_CATEGORY.MULTIFACTOR_AUTH);
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThatExceptionOfType(WingsException.class);
    }

    try {
      userService.verifyJWTToken("fakeData", JWT_CATEGORY.MULTIFACTOR_AUTH);
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      Assertions.assertThatExceptionOfType(WingsException.class);
    }
  }
}
