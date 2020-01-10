package software.wings.service;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.EmailVerificationToken.Builder.anEmailVerificationToken;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;
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
import static software.wings.utils.WingsTestConstants.INVALID_USER_EMAIL;
import static software.wings.utils.WingsTestConstants.NEW_USER_EMAIL;
import static software.wings.utils.WingsTestConstants.NEW_USER_NAME;
import static software.wings.utils.WingsTestConstants.NOTE;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
import static software.wings.utils.WingsTestConstants.SUPPORT_EMAIL;
import static software.wings.utils.WingsTestConstants.TEMPORARY_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_INVITE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.USER_PASSWORD;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import freemarker.template.TemplateException;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.model.EventType;
import io.harness.exception.GeneralException;
import io.harness.exception.HintException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.UserRegistrationException;
import io.harness.exception.WingsException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.mail.EmailException;
import org.apache.commons.validator.routines.UrlValidator;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.AccountJoinRequest;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.Event.Type;
import software.wings.beans.MarketPlace;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.UserInvite;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.beans.utm.UtmInfo;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.AwsMarketPlaceApiHandlerImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.UserServiceLimitChecker;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.signup.SignupException;
import software.wings.signup.BlackListedDomainChecker;
import software.wings.signup.SignupServiceImpl;
import software.wings.utils.CacheManager;
import software.wings.utils.WingsTestConstants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserServiceTest extends WingsBaseTest {
  private final User.Builder userBuilder = anUser().appId(APP_ID).email(USER_EMAIL).name(USER_NAME).password(PASSWORD);

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
  @Mock private UserGroupService userGroupService;
  @Mock private CacheManager cacheManager;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private UserServiceLimitChecker userServiceLimitChecker;
  @Mock private LoginSettingsService loginSettingsService;
  @Mock private BlackListedDomainChecker blackListedDomainChecker;
  @Mock private EventPublishHelper eventPublishHelper;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Spy @InjectMocks private SignupServiceImpl signupService;

  /**
   * The Cache.
   */
  @Mock Cache<String, User> cache;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;
  @Inject @InjectMocks private UserService userService;
  @InjectMocks private UserService mockedUserService = mock(UserServiceImpl.class);
  @Captor private ArgumentCaptor<EmailData> emailDataArgumentCaptor;
  @Captor private ArgumentCaptor<User> userArgumentCaptor;
  @Captor private ArgumentCaptor<PageRequest<User>> pageRequestArgumentCaptor;
  @Inject @InjectMocks SecretManager secretManager;
  @Inject @InjectMocks private AwsMarketPlaceApiHandlerImpl marketPlaceService;
  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(configuration.getSupportEmail()).thenReturn(SUPPORT_EMAIL);
    doNothing().when(userServiceLimitChecker).limitCheck(Mockito.anyString(), Mockito.anyList(), Mockito.anySet());
    when(cacheManager.getUserCache()).thenReturn(cache);

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
    when(limitCheckerFactory.getInstance(Mockito.any(io.harness.limits.Action.class)))
        .thenReturn(WingsTestConstants.mockChecker());

    when(configuration.isBlacklistedEmailDomainsAllowed()).thenReturn(true);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testMarketPlaceSignUp() {
    when(configuration.getPortal().getJwtMarketPlaceSecret()).thenReturn("TESTSECRET");
    UserInvite testInvite = anUserInvite().withUuid(USER_INVITE_ID).withEmail(USER_EMAIL).build();
    testInvite.setPassword("TestPassword".toCharArray());
    MarketPlace marketPlace = MarketPlace.builder()
                                  .uuid("TESTUUID")
                                  .type(MarketPlaceType.AWS)
                                  .orderQuantity(10)
                                  .expirationDate(new Date())
                                  .build();
    String token = marketPlaceService.getMarketPlaceToken(marketPlace, testInvite);

    User savedUser = userBuilder.uuid(USER_ID)
                         .email(USER_EMAIL)
                         .emailVerified(false)
                         .companyName(COMPANY_NAME)
                         .accountName(ACCOUNT_NAME)
                         .passwordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                         .build();

    try {
      userService.completeMarketPlaceSignup(savedUser, testInvite, MarketPlaceType.AWS);
      fail("");
    } catch (Exception e) {
      log().info("Expected error " + e.getMessage());
      assertThat(e).isInstanceOf(UnauthorizedException.class);
    }

    when(wingsPersistence.get(UserInvite.class, USER_INVITE_ID)).thenReturn(testInvite);
    when(userService.getUserByEmail(USER_EMAIL)).thenReturn(savedUser);
    try {
      userService.completeMarketPlaceSignup(savedUser, testInvite, MarketPlaceType.AWS);
      fail("");
    } catch (WingsException e) {
      log().info("Expected error " + e.getCode());
      assertThat(e).isInstanceOf(UserRegistrationException.class);
    }

    when(userService.getUserByEmail(USER_EMAIL)).thenReturn(null);
    try {
      userService.completeMarketPlaceSignup(savedUser, testInvite, MarketPlaceType.AWS);
      fail("");
    } catch (WingsException e) {
      log().info("Expected error " + e.getCode());
      assertThat(e).isInstanceOf(GeneralException.class);
    }

    testInvite.setMarketPlaceToken("fakeToken");

    try {
      userService.completeMarketPlaceSignup(savedUser, testInvite, MarketPlaceType.AWS);
      fail("");
    } catch (WingsException e) {
      log().info("Expected error " + e.getCode());
      assertThat(e).isInstanceOf(HintException.class);
    }

    testInvite.setMarketPlaceToken(token);

    Account account = Account.Builder.anAccount()
                          .withAccountName(ACCOUNT_NAME)
                          .withCompanyName(COMPANY_NAME)
                          .withUuid(ACCOUNT_ID)
                          .build();

    when(accountService.save(any(Account.class), eq(false))).thenReturn(account);
    when(wingsPersistence.saveAndGet(any(Class.class), any(User.class))).thenReturn(savedUser);
    when(wingsPersistence.get(MarketPlace.class, "TESTUUID")).thenReturn(marketPlace);
    when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean())).thenReturn(aPageResponse().build());
    when(authenticationManager.defaultLogin(USER_EMAIL, "TestPassword")).thenReturn(savedUser);
    User user = userService.completeMarketPlaceSignup(savedUser, testInvite, MarketPlaceType.AWS);
    assertThat(user).isEqualTo(savedUser);
  }

  /**
   * Test register.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldRegisterNewUser() {
    User savedUser = userBuilder.uuid(USER_ID)
                         .emailVerified(false)
                         .companyName(COMPANY_NAME)
                         .accountName(ACCOUNT_NAME)
                         .passwordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                         .build();
    Account account = Account.Builder.anAccount()
                          .withAccountName(ACCOUNT_NAME)
                          .withCompanyName(COMPANY_NAME)
                          .withUuid(ACCOUNT_ID)
                          .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(configuration.getPortal().getVerificationUrl()).thenReturn(VERIFICATION_PATH);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(new EmailVerificationToken(USER_ID));
    when(accountService.save(any(Account.class), eq(false))).thenReturn(account);
    when(wingsPersistence.query(eq(User.class), any(PageRequest.class))).thenReturn(aPageResponse().build());
    when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean())).thenReturn(aPageResponse().build());

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

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldIncludeEnvPathInTrialSignupEmailUrl() {
    when(configuration.isTrialRegistrationAllowed()).thenReturn(true);
    when(configuration.getPortal().getUrl()).thenReturn("https://qa.harness.io");

    String inviteId = UUIDGenerator.generateUuid();
    when(wingsPersistence.save(any(UserInvite.class))).thenReturn(inviteId);

    userService.trialSignup(USER_EMAIL);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());

    String templateUrl = ((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url");
    assertThat(templateUrl).isNotNull();
    assertThat(UrlValidator.getInstance().isValid(templateUrl)).isTrue();
    assertThat(templateUrl.startsWith("https://qa.harness.io/#")).isTrue();
    assertThat(templateUrl.contains("inviteId=" + inviteId)).isTrue();
    assertThat(templateUrl.contains("email=" + USER_EMAIL)).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBlockBlacklistedEmailRegistration() {
    when(configuration.isBlacklistedEmailDomainsAllowed()).thenReturn(false);
    when(configuration.isTrialRegistrationAllowed()).thenReturn(true);
    doThrow(new SignupException("Invalid domain")).when(blackListedDomainChecker).check(Mockito.anyString());
    try {
      userService.trialSignup(TEMPORARY_EMAIL);
      fail("Temporary is not allowed for trial signup");
    } catch (SignupException e) {
      // Exception is expected as temporary emails is not allowed.
    }
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testNewUserSignup() {
    when(configuration.isTrialRegistrationAllowed()).thenReturn(true);
    when(configuration.getPortal().getUrl()).thenReturn("https://qa.harness.io");
    doNothing().when(signupService).validatePassword(any());

    String inviteId = UUIDGenerator.generateUuid();
    when(wingsPersistence.save(any(UserInvite.class))).thenReturn(inviteId);

    String email = "testuser@account10.com";
    String accountName = "ACCOUNT10";
    String userName = "testuser";
    UserInvite userInvite = anUserInvite()
                                .withAccountName(accountName)
                                .withCompanyName("COMPANY10")
                                .withEmail(email)
                                .withName(userName)
                                .build();
    userInvite.setPassword("password".toCharArray());
    UtmInfo utmInfo = UtmInfo.builder().utmCampaign("campaign").utmContent("content").utmSource("source").build();
    userInvite.setUtmInfo(utmInfo);
    userService.trialSignup(userInvite);
    verify(eventPublishHelper).publishTrialUserSignupEvent(utmInfo, email, userName, inviteId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testValidateTrialSignupEmailWithIllegalChars() {
    when(configuration.isBlacklistedEmailDomainsAllowed()).thenReturn(false);
    when(configuration.isTrialRegistrationAllowed()).thenReturn(true);

    for (Character illegalChar : userService.ILLEGAL_CHARACTERS) {
      try {
        String email = "test" + illegalChar + "User@abc.com";
        userService.trialSignup(email);
        fail("Email with illegal character is not allowed for trial signup");
      } catch (WingsException e) {
        // Exception is expected as temporary emails is not allowed.
      }
    }
  }

  /**
   * Test register for existing user.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldRegisterExistingUser() {
    User existingUser = userBuilder.uuid(generateUuid()).build();
    User savedUser = userBuilder.uuid(USER_ID)
                         .emailVerified(false)
                         .companyName(COMPANY_NAME)
                         .accountName(ACCOUNT_NAME)
                         .passwordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                         .build();
    Account account = Account.Builder.anAccount()
                          .withAccountName(ACCOUNT_NAME)
                          .withCompanyName(COMPANY_NAME)
                          .withUuid(ACCOUNT_ID)
                          .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(configuration.getPortal().getVerificationUrl()).thenReturn(VERIFICATION_PATH);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(accountService.save(any(Account.class), eq(false))).thenReturn(account);
    when(wingsPersistence.query(eq(User.class), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(Lists.newArrayList(existingUser)).build());
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(anEmailVerificationToken().withToken("token123").build());
    when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean())).thenReturn(aPageResponse().build());

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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldMatchPassword() {
    String hashpw = hashpw(new String(PASSWORD), BCrypt.gensalt());
    assertThat(userService.matchPassword(PASSWORD, hashpw)).isTrue();
  }

  /**
   * Should update user.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldUpdateUser() {
    List<Role> roles = Lists.newArrayList(
        aRole().withUuid(generateUuid()).withRoleType(RoleType.APPLICATION_ADMIN).withAppId(generateUuid()).build());
    User user = anUser().appId(APP_ID).uuid(USER_ID).email(USER_EMAIL).name(USER_NAME).roles(roles).build();
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set("name", USER_NAME);
    updateOperations.set("roles", roles);

    userService.update(user);
    verify(wingsPersistence).update(user, updateOperations);
    verify(wingsPersistence).getWithAppId(User.class, APP_ID, USER_ID);
    verify(cache).remove(USER_ID);
  }

  /**
   * Should update user profile.
   */
  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void shouldUpdateUserProfile() {
    User user = anUser().appId(APP_ID).uuid(USER_ID).appId(APP_ID).name(USER_NAME).build();
    userService.updateUserProfile(user);
    verify(updateOperations).set(UserKeys.name, USER_NAME);
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set(UserKeys.name, USER_NAME);
    verify(wingsPersistence).update(user, updateOperations);
    verify(wingsPersistence).getWithAppId(User.class, APP_ID, USER_ID);
    verify(cache).remove(USER_ID);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void shouldUpdateUserProfileWithoutName() {
    User user = anUser().appId(APP_ID).uuid(USER_ID).appId(APP_ID).build();
    userService.updateUserProfile(user);
    verify(updateOperations).unset(UserKeys.name);
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.unset(UserKeys.name);
    verify(wingsPersistence).update(user, updateOperations);
    verify(wingsPersistence).getWithAppId(User.class, APP_ID, USER_ID);
    verify(cache).remove(USER_ID);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void addEventToUserMarketoCampaigns() {
    EventType eventType = EventType.USER_INVITED_FROM_EXISTING_ACCOUNT;
    User user = anUser().appId(APP_ID).uuid(USER_ID).appId(APP_ID).build();
    doReturn(user).when(cache).get(any(String.class));
    userService.addEventToUserMarketoCampaigns(user.getUuid(), eventType);
    verify(updateOperations).set(UserKeys.reportedMarketoCampaigns, Sets.newHashSet(eventType.name()));

    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set(UserKeys.reportedMarketoCampaigns, Sets.newHashSet(eventType.name()));
    verify(wingsPersistence).update(user, updateOperations);
    verify(wingsPersistence).getWithAppId(User.class, APP_ID, USER_ID);
    verify(cache).remove(USER_ID);
  }

  /**
   * Should list users.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldListUsers() {
    PageRequest<User> request = new PageRequest<>();
    request.addFilter("appId", EQ, GLOBAL_APP_ID);
    userService.list(request, false);
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDeleteUser() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).build());
    when(wingsPersistence.delete(User.class, USER_ID)).thenReturn(true);
    userService.delete(ACCOUNT_ID, USER_ID);
    verify(wingsPersistence).delete(User.class, USER_ID);
    verify(cache).remove(USER_ID);
    verify(auditServiceHelper, times(1)).reportDeleteForAuditingUsingAccountId(eq(ACCOUNT_ID), any(User.class));
  }

  /**
   * Should fetch user.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldFetchUser() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).build());
    User user = userService.get(USER_ID);
    verify(wingsPersistence).get(User.class, USER_ID);
    assertThat(user).isEqualTo(userBuilder.uuid(USER_ID).build());
  }

  /**
   * Should throw exception if user does not exist.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldThrowExceptionIfUserDoesNotExist() {
    assertThatThrownBy(() -> userService.get("INVALID_USER_ID"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("User does not exist");
  }

  /**
   * Should verify email.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldSendEmail() throws EmailException, TemplateException, IOException {
    emailDataNotificationService.send(
        EmailData.builder().to(asList("anubhaw@gmail.com")).subject("wings-test").body("hi").build());
  }

  /**
   * Test assign role to user.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldAddRole() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).build());
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldRevokeRole() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).build());
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldInviteNewUser() {
    UserInvite userInvite = anUserInvite()
                                .withAppId(GLOBAL_APP_ID)
                                .withAccountId(ACCOUNT_ID)
                                .withEmails(asList(USER_EMAIL))
                                .withRoles(asList(aRole().withUuid(ROLE_ID).build()))
                                .build();
    Account account = Account.Builder.anAccount()
                          .withAccountName(ACCOUNT_NAME)
                          .withCompanyName(COMPANY_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                          .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(wingsPersistence.save(userInvite)).thenReturn(USER_INVITE_ID);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(userBuilder.uuid(USER_ID).build());

    userService.inviteUsers(userInvite);
    verify(wingsPersistence).save(any(UserInvite.class));
    verify(wingsPersistence).getWithAppId(UserInvite.class, GLOBAL_APP_ID, USER_INVITE_ID);
    verify(wingsPersistence).saveAndGet(eq(User.class), any(User.class));
    verify(cache).remove(USER_ID);
    verify(auditServiceHelper, times(userInvite.getEmails().size()))
        .reportForAuditingUsingAccountId(eq(ACCOUNT_ID), eq(null), any(UserInvite.class), eq(Type.CREATE));

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
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldInviteNewUserMixedCaseEmail() throws EmailException, TemplateException, IOException {
    String mixedEmail = "UseR@wings.software ";
    UserInvite userInvite = anUserInvite()
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
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(userBuilder.uuid(USER_ID).build());

    userService.inviteUsers(userInvite);

    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(userArgumentCaptor.getValue()).hasFieldOrPropertyWithValue("email", mixedEmail.trim().toLowerCase());
    verify(auditServiceHelper, times(userInvite.getEmails().size()))
        .reportForAuditingUsingAccountId(eq(ACCOUNT_ID), eq(null), any(UserInvite.class), eq(Type.CREATE));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void testInviteNewUser_invalidEmail_shouldFail() {
    UserInvite userInvite = anUserInvite()
                                .withAppId(GLOBAL_APP_ID)
                                .withAccountId(ACCOUNT_ID)
                                .withEmails(asList(INVALID_USER_EMAIL))
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
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(userBuilder.uuid(USER_ID).build());

    try {
      userService.inviteUsers(userInvite);
      fail("Exception is expected when inviting with invalid user email");
    } catch (WingsException e) {
      // Ignore, exception expected here.
    }
  }

  /**
   * Should invite existing user.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldInviteExistingUser() {
    UserInvite userInvite = anUserInvite()
                                .withAppId(GLOBAL_APP_ID)
                                .withAccountId(ACCOUNT_ID)
                                .withEmails(asList(USER_EMAIL))
                                .withRoles(asList(aRole().withUuid(ROLE_ID).build()))
                                .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withCompanyName(COMPANY_NAME).withUuid(ACCOUNT_ID).build());
    when(query.get()).thenReturn(userBuilder.uuid(USER_ID).build());
    when(wingsPersistence.save(userInvite)).thenReturn(USER_INVITE_ID);

    userService.inviteUsers(userInvite);

    verify(query, times(2)).field("email");
    verify(end, times(2)).equal(USER_EMAIL);
    verify(accountService).get(ACCOUNT_ID);
    verify(wingsPersistence).save(userInvite);
    verify(wingsPersistence).getWithAppId(UserInvite.class, GLOBAL_APP_ID, USER_INVITE_ID);
  }

  /**
   * Should Override 2FA on user
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldOverrideTwoFactorForUser() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).build());
    when(wingsPersistence.get(Account.class, ACCOUNT_ID))
        .thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).build());
    // userService.overrideTwoFactorforAccount(ACCOUNT_ID, true);
  }

  /**
   * Should complete invite.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCompleteInvite() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).build());
    when(accountService.get(ACCOUNT_ID)).thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).build());
    when(userInviteQuery.get())
        .thenReturn(anUserInvite().withUuid(USER_INVITE_ID).withAccountId(ACCOUNT_ID).withEmail(USER_EMAIL).build());
    when(query.get()).thenReturn(userBuilder.uuid(USER_ID).build());
    when(loginSettingsService.verifyPasswordStrength(Mockito.any(Account.class), Mockito.any(char[].class)))
        .thenReturn(true);

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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetAccountRole() {
    List<Role> roles =
        asList(aRole().withUuid(generateUuid()).withRoleType(RoleType.ACCOUNT_ADMIN).withAccountId(ACCOUNT_ID).build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).roles(roles).build());
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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetAccountForAllApsAdminRole() {
    List<Role> roles = asList(aRole()
                                  .withUuid(generateUuid())
                                  .withRoleType(RoleType.APPLICATION_ADMIN)
                                  .withAccountId(ACCOUNT_ID)
                                  .withAllApps(true)
                                  .build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).roles(roles).build());
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetApplicationRole() {
    List<Role> roles =
        asList(aRole().withUuid(generateUuid()).withRoleType(RoleType.ACCOUNT_ADMIN).withAccountId(ACCOUNT_ID).build());
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.uuid(USER_ID).roles(roles).build());
    when(appService.get(APP_ID)).thenReturn(anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build());

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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldSendResetPasswordEmail() throws EmailException, TemplateException, IOException {
    ArrayList<Account> accounts = new ArrayList<>();
    accounts.add(new Account());
    when(query.get()).thenReturn(userBuilder.uuid(USER_ID).accounts(accounts).build());
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

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldJoinAccount() {
    ArrayList<Account> accounts = new ArrayList<>();
    accounts.add(new Account());
    when(query.get()).thenReturn(userBuilder.uuid(USER_ID).email(NEW_USER_EMAIL).accounts(accounts).build());
    when(configuration.getPortal().getJwtPasswordSecret()).thenReturn("SECRET");
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(configuration.isTrialRegistrationAllowed()).thenReturn(true);
    when(configuration.getSupportEmail()).thenReturn(SUPPORT_EMAIL);
    doNothing().when(blackListedDomainChecker).check(Mockito.anyString());

    AccountJoinRequest accountJoinRequest = AccountJoinRequest.builder()
                                                .email(NEW_USER_EMAIL)
                                                .name(NEW_USER_NAME)
                                                .note(NOTE)
                                                .companyName(COMPANY_NAME)
                                                .build();
    userService.accountJoinRequest(accountJoinRequest);
    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(SUPPORT_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getCc()).isEmpty();
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("msg"))
        .asString()
        .startsWith("Recipient is not a Harness user in production cluster - ");
  }

  /**
   * Should update password.
   *
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldUpdatePassword() throws UnsupportedEncodingException {
    when(query.get()).thenReturn(userBuilder.uuid(USER_ID).build());
    when(configuration.getPortal().getJwtPasswordSecret()).thenReturn("SECRET");
    Algorithm algorithm = Algorithm.HMAC256("SECRET");
    String token = JWT.create()
                       .withIssuer("Harness Inc")
                       .withIssuedAt(new Date())
                       .withExpiresAt(new Date(System.currentTimeMillis() + 4 * 60 * 60 * 1000)) // 4 hrs
                       .withClaim("email", USER_EMAIL)
                       .sign(algorithm);

    userService.updatePassword(token, USER_PASSWORD);

    verify(query, times(1)).filter("email", USER_EMAIL);
    verify(authService).invalidateAllTokensForUser(USER_ID);
    verify(wingsPersistence).update(eq(userBuilder.uuid(USER_ID).build()), any(UpdateOperations.class));
    verify(updateOperations).set(eq("passwordHash"), anyString());
    verify(updateOperations).set(eq("passwordChangedAt"), anyLong());
  }

  /**
   * Should add Account.
   *
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldAddAccount() throws UnsupportedEncodingException {
    Account account =
        anAccount().withUuid(ACCOUNT_ID).withCompanyName(COMPANY_NAME).withAccountName(ACCOUNT_NAME).build();
    when(accountService.save(any(Account.class), false)).thenReturn(account);
    when(roleService.getAccountAdminRole(ACCOUNT_ID)).thenReturn(aRole().build());
    when(updateOperations.addToSet(any(), any())).thenReturn(updateOperations);
    User user = anUser().uuid(USER_ID).email(USER_EMAIL).build();
    Account created = userService.addAccount(account, user, true);
    assertThat(created).isEqualTo(account);
    verify(accountService).exists(eq(ACCOUNT_NAME));
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
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
      // Ignore
    }

    try {
      userService.verifyJWTToken("fakeData", JWT_CATEGORY.MULTIFACTOR_AUTH);
      Assertions.failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      // Ignore
    }
  }

  /**
   * Should resend the invitation email
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void resendInvitationEmail() {
    User user = userBuilder.uuid(USER_ID)
                    .emailVerified(false)
                    .companyName(COMPANY_NAME)
                    .accountName(ACCOUNT_NAME)
                    .passwordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                    .build();
    UserInvite userInvite =
        anUserInvite().withAppId(GLOBAL_APP_ID).withAccountId(ACCOUNT_ID).withEmail(USER_EMAIL).build();
    Account account = anAccount()
                          .withCompanyName(COMPANY_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                          .build();
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(wingsPersistence.save(userInvite)).thenReturn(USER_INVITE_ID);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(mockedUserService.getUserByEmail(USER_EMAIL)).thenReturn(user);
    when(mockedUserService.deleteInvites(ACCOUNT_ID, USER_EMAIL)).thenCallRealMethod();
    doCallRealMethod().when(mockedUserService).sendNewInvitationMail(userInvite, account);
    userService.resendInvitationEmail(mockedUserService, ACCOUNT_ID, USER_EMAIL);
    verify(accountService).get(ACCOUNT_ID);
    verify(wingsPersistence).delete(any(Query.class));
    verify(wingsPersistence).save(userInvite);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo(INVITE_EMAIL_TEMPLATE_NAME);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testUserVerified() {
    Account account = anAccount()
                          .withCompanyName(COMPANY_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                          .build();
    User user = userBuilder.uuid(USER_ID)
                    .emailVerified(false)
                    .companyName(COMPANY_NAME)
                    .accountName(ACCOUNT_NAME)
                    .passwordHash(hashpw(new String(PASSWORD), BCrypt.gensalt()))
                    .accounts(Arrays.asList(account))
                    .build();

    assertThat(userService.isUserVerified(user)).isFalse();
    user.setEmailVerified(true);
    assertThat(userService.isUserVerified(user)).isTrue();

    account.setAuthenticationMechanism(AuthenticationMechanism.LDAP);
    user.setEmailVerified(false);
    assertThat(userService.isUserVerified(user)).isTrue();

    account.setAuthenticationMechanism(AuthenticationMechanism.SAML);
    assertThat(userService.isUserVerified(user)).isTrue();
  }
}
