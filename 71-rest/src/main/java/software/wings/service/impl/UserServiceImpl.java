package software.wings.service.impl;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.HAS;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.String.format;
import static java.sql.Date.from;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AccountRole.AccountRoleBuilder.anAccountRole;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ApplicationRole.ApplicationRoleBuilder.anApplicationRole;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.WORKFLOW;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.encoding.EncodingUtils;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.model.EventType;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.UserRegistrationException;
import io.harness.exception.WingsException;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.marketplace.gcp.procurement.GcpProcurementService;
import io.harness.persistence.UuidAware;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountJoinRequest;
import software.wings.beans.AccountRole;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.ApplicationRole;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.EmailVerificationToken.EmailVerificationTokenKeys;
import software.wings.beans.EntityType;
import software.wings.beans.Event;
import software.wings.beans.Event.Type;
import software.wings.beans.LicenseInfo;
import software.wings.beans.MarketPlace;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.beans.User.UserKeys;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.UserInvite.UserInviteKeys;
import software.wings.beans.UserInviteSource;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.PasswordSource;
import software.wings.beans.loginSettings.PasswordStrengthViolations;
import software.wings.beans.loginSettings.UserLockoutInfo;
import software.wings.beans.marketplace.MarketPlaceConstants;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.licensing.LicenseService;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.OauthProviderType;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;
import software.wings.security.authentication.oauth.OauthUserInfo;
import software.wings.security.saml.SamlClientService;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.marketplace.gcp.GCPBillingPollingService;
import software.wings.service.intfc.signup.SignupException;
import software.wings.service.intfc.signup.SignupSpamChecker;
import software.wings.utils.CacheManager;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/9/16.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class UserServiceImpl implements UserService {
  public static final String ADD_GROUP_EMAIL_TEMPLATE_NAME = "add_group";
  public static final String SIGNUP_EMAIL_TEMPLATE_NAME = "signup";
  public static final String INVITE_EMAIL_TEMPLATE_NAME = "invite";
  public static final String TRIAL_EMAIL_VERIFICATION_TEMPLATE_NAME = "invite_trial";
  public static final String JOIN_EXISTING_TEAM_TEMPLATE_NAME = "join_existing_team";
  public static final int REGISTRATION_SPAM_THRESHOLD = 3;
  public static final String EXC_MSG_RESET_PASS_LINK_NOT_GEN = "Reset password link could not be generated";
  public static final String EXC_MSG_USER_DOESNT_EXIST = "User does not exist";
  public static final String EXC_MSG_USER_INVITE_INVALID =
      "User was not invited to access account or the invitation is obsolete";
  public static final String EXC_USER_ALREADY_REGISTERED = "User is already registered";

  /**
   * The Executor service.
   */
  @Inject ExecutorService executorService;
  @Inject private UserServiceLimitChecker userServiceLimitChecker;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private MainConfiguration configuration;
  @Inject private RoleService roleService;
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private AuthService authService;
  @Inject private UserGroupService userGroupService;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private AppService appService;
  @Inject private CacheManager cacheManager;
  @Inject private AuthHandler authHandler;
  @Inject private SecretManager secretManager;
  @Inject private TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private SamlClientService samlClientService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private UsageMetricsEventPublisher usageMetricsEventPublisher;
  @Inject private AuthenticationManager authenticationManager;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private SSOService ssoService;
  @Inject private LoginSettingsService loginSettingsService;
  @Inject private LimitConfigurationService limits;
  @Inject private GCPBillingPollingService gcpBillingPollingService;
  @Inject private GcpProcurementService gcpProcurementService;
  @Inject private SignupService signupService;
  @Inject private SignupSpamChecker spamChecker;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#register(software.wings.beans.User)
   */
  @Override
  public User register(User user) {
    if (isNotBlank(user.getEmail())) {
      user.setEmail(user.getEmail().trim().toLowerCase());
    }

    if (isNotBlank(user.getAccountName())) {
      user.setAccountName(user.getAccountName().trim());
    }

    if (isNotBlank(user.getName())) {
      user.setName(user.getName().trim());
    }

    if (isNotBlank(user.getCompanyName())) {
      user.setCompanyName(user.getCompanyName().trim());
    }

    verifyRegisteredOrAllowed(user.getEmail());

    Account account = setupTrialAccount(user.getAccountName(), user.getCompanyName());

    User savedUser = registerNewUser(user, account);
    executorService.execute(() -> sendVerificationEmail(savedUser));
    eventPublishHelper.publishUserInviteFromAccountEvent(account.getUuid(), savedUser.getEmail());
    return savedUser;
  }

  @Override
  public UserInvite createUserInviteForMarketPlace() {
    UserInvite userInvite = new UserInvite();
    userInvite.setSource(UserInviteSource.builder().type(SourceType.MARKETPLACE).build());
    userInvite.setCompleted(false);

    String inviteId = wingsPersistence.save(userInvite);
    userInvite.setUuid(inviteId);

    logger.info("Created a new user invite {} for a signup request from market place", inviteId);

    return userInvite;
  }

  /**
   * Trial/Freemium user invitation won't create account. The freemium account will be created only at time of
   * invitation completion.
   */
  @Override
  public boolean trialSignup(UserInvite userInvite) {
    final String emailAddress = userInvite.getEmail().toLowerCase();
    validateTrialSignup(emailAddress);
    signupService.validatePassword(userInvite.getPassword());

    UserInvite userInviteInDB = signupService.getUserInviteByEmail(emailAddress);

    Map<String, String> params = new HashMap<>();
    params.put("email", userInvite.getEmail());
    String url = "/activation.html";

    if (userInviteInDB == null) {
      userInvite.setSource(UserInviteSource.builder().type(SourceType.TRIAL).build());
      userInvite.setCompleted(false);
      String hashed = hashpw(new String(userInvite.getPassword()), BCrypt.gensalt());
      userInvite.setPasswordHash(hashed);

      String inviteId = wingsPersistence.save(userInvite);
      userInvite.setUuid(inviteId);
      params.put("userInviteId", inviteId);

      logger.info("Created a new user invite {} for company {}", inviteId, userInvite.getCompanyName());

      // Send an email invitation for the trial user to finish up the sign-up with additional information
      // such as password, account/company name information.
      sendVerificationEmail(userInvite, url, params);
      eventPublishHelper.publishTrialUserSignupEvent(
          userInvite.getUtmInfo(), emailAddress, userInvite.getName(), inviteId);
    } else if (userInviteInDB.isCompleted()) {
      if (spamChecker.isSpam(userInviteInDB)) {
        return false;
      }
      // HAR-7590: If user invite has completed. Send an email saying so and ask the user to login directly.
      signupService.sendTrialSignupCompletedEmail(userInviteInDB);
    } else {
      if (spamChecker.isSpam(userInviteInDB)) {
        return false;
      }

      params.put("userInviteId", userInviteInDB.getUuid());
      // HAR-7250: If the user invite was not completed. Resend the verification/invitation email.
      sendVerificationEmail(userInviteInDB, url, params);
    }
    return true;
  }

  @Override
  public boolean accountJoinRequest(AccountJoinRequest accountJoinRequest) {
    final String emailAddress = accountJoinRequest.getEmail().toLowerCase();
    signupService.validateEmail(emailAddress);
    Map<String, String> params = new HashMap<>();
    params.put("email", emailAddress);
    params.put("name", accountJoinRequest.getName());
    params.put("url", "https://app.harness.io/#/login");
    params.put("companyName", accountJoinRequest.getCompanyName());
    params.put("note", accountJoinRequest.getNote());
    String accountAdminEmail = accountJoinRequest.getAccountAdminEmail();
    boolean hasAccountAdminEmail = isNotEmpty(accountAdminEmail);
    User user = hasAccountAdminEmail ? getUserByEmail(accountAdminEmail) : null;
    String supportEmail = configuration.getSupportEmail();
    String to = supportEmail;
    String msg = "";
    if (user != null) {
      boolean isValidUser = hasAccountAdminEmail && isUserVerified(user);
      boolean isAdmin = isValidUser && isUserAdminOfAnyAccount(user);

      if (isAdmin) {
        to = accountAdminEmail;
      } else if (isValidUser) {
        to = supportEmail;
        msg = "Recipient is not a Harness admin in production cluster - ";
      } else {
        to = supportEmail;
        msg = "Recipient is not a Harness user in production cluster - ";
      }
    } else {
      msg = "Recipient is not a Harness user in production cluster - ";
    }

    params.put("msg", msg);
    boolean emailSent = sendEmail(to, JOIN_EXISTING_TEAM_TEMPLATE_NAME, params);
    eventPublishHelper.publishJoinAccountEvent(emailAddress, accountJoinRequest.getName());
    return emailSent;
  }

  @Override
  public boolean postCustomEvent(String accountId, String event) {
    eventPublishHelper.publishCustomEvent(accountId, event);
    return true;
  }

  /**
   * Trial/Freemium user invitation won't create account. The freemium account will be created only at time of
   * invitation completion.
   */
  @Override
  public boolean trialSignup(String email) {
    final String emailAddress = email.trim().toLowerCase();
    validateTrialSignup(emailAddress);

    UserInvite userInvite = signupService.getUserInviteByEmail(emailAddress);
    if (userInvite == null) {
      // Create a new user invite to track the invitation status
      userInvite = new UserInvite();
      userInvite.setSource(UserInviteSource.builder().type(SourceType.TRIAL).build());
      userInvite.setEmail(emailAddress);
      userInvite.setCompleted(false);

      String inviteId = wingsPersistence.save(userInvite);
      userInvite.setUuid(inviteId);

      String url = format("/invite?email=%s&inviteId=%s", userInvite.getEmail(), userInvite.getUuid());
      // Send an email invitation for the trial user to finish up the sign-up with additional information
      // such as password, account/company name information.
      sendVerificationEmail(userInvite, url);
      eventPublishHelper.publishTrialUserSignupEvent(emailAddress, null, inviteId);
    } else if (userInvite.isCompleted()) {
      if (spamChecker.isSpam(userInvite)) {
        return false;
      }
      // HAR-7590: If user invite has completed. Send an email saying so and ask the user to login directly.
      signupService.sendTrialSignupCompletedEmail(userInvite);
    } else {
      if (spamChecker.isSpam(userInvite)) {
        return false;
      }

      String url = format("/invite?email=%s&inviteId=%s", userInvite.getEmail(), userInvite.getUuid());
      // HAR-7250: If the user invite was not completed. Resend the verification/invitation email.
      sendVerificationEmail(userInvite, url);
    }

    return true;
  }

  private void validateTrialSignup(String email) {
    signupService.validateCluster();
    signupService.validateEmail(email);
  }

  @Override
  public Account addAccount(Account account, User user, boolean addUser) {
    if (isNotBlank(account.getAccountName())) {
      account.setAccountName(account.getAccountName().trim());
    }

    if (isNotBlank(account.getCompanyName())) {
      account.setCompanyName(account.getCompanyName().trim());
    }

    account = setupAccount(account);
    if (addUser) {
      addAccountAdminRole(user, account);
      authHandler.addUserToDefaultAccountAdminUserGroup(user, account, true);
      sendSuccessfullyAddedToNewAccountEmail(user, account);
      evictUserFromCache(user.getUuid());
    }
    return account;
  }

  @Override
  public User getUserSummary(User user) {
    if (user == null) {
      return null;
    }
    User userSummary = new User();
    userSummary.setName(user.getName());
    userSummary.setUuid(user.getUuid());
    userSummary.setEmail(user.getEmail());
    userSummary.setEmailVerified(user.isEmailVerified());
    userSummary.setTwoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled());
    userSummary.setUserLocked(user.isUserLocked());
    userSummary.setPasswordExpired(user.isPasswordExpired());
    userSummary.setImported(user.isImported());
    return userSummary;
  }

  @Override
  public List<User> getUserSummary(List<User> userList) {
    if (isEmpty(userList)) {
      return Collections.emptyList();
    }
    return userList.stream().map(this ::getUserSummary).collect(toList());
  }

  private void sendSuccessfullyAddedToNewAccountEmail(User user, Account account) {
    try {
      String loginUrl = buildAbsoluteUrl(format("/login?company=%s&account=%s&email=%s", account.getCompanyName(),
                                             account.getAccountName(), user.getEmail()),
          account.getUuid());

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("name", user.getName());
      templateModel.put("url", loginUrl);
      templateModel.put("company", account.getCompanyName());
      List<String> toList = new ArrayList();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName("add_account")
                                .templateModel(templateModel)
                                .accountId(account.getUuid())
                                .system(false)
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);
      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Add account email couldn't be sent", e);
    }
  }

  @Override
  public User registerNewUser(User user, Account account) {
    String accountId = account.getUuid();

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new io.harness.limits.Action(accountId, ActionType.CREATE_USER));

    User existingUser = getUserByEmail(user.getEmail());
    if (existingUser == null) {
      return LimitEnforcementUtils.withLimitCheck(checker, () -> {
        user.setAppId(GLOBAL_APP_ID);
        user.getAccounts().add(account);
        user.setEmailVerified(false);
        String hashed = hashpw(new String(user.getPassword()), BCrypt.gensalt());
        user.setPasswordHash(hashed);
        user.setPasswordChangedAt(System.currentTimeMillis());
        user.setRoles(Lists.newArrayList(roleService.getAccountAdminRole(account.getUuid())));
        return save(user, accountId);
      });

    } else {
      Map<String, Object> map = new HashMap<>();
      map.put("name", user.getName());
      map.put("passwordHash", hashpw(new String(user.getPassword()), BCrypt.gensalt()));
      wingsPersistence.updateFields(User.class, existingUser.getUuid(), map);
      return existingUser;
    }
  }

  @Override
  public User getUserByEmail(String email) {
    User user = null;
    if (isNotEmpty(email)) {
      user = wingsPersistence.createQuery(User.class).filter(UserKeys.email, email.trim().toLowerCase()).get();
      loadSupportAccounts(user);
    }

    return user;
  }

  @Override
  public User getUserByEmail(String email, String accountId) {
    User user = null;
    if (isNotEmpty(email)) {
      user = wingsPersistence.createQuery(User.class)
                 .filter(UserKeys.email, email.trim().toLowerCase())
                 .field(UserKeys.accounts)
                 .hasThisOne(accountId)
                 .get();
      loadSupportAccounts(user);
    }

    return user;
  }

  @Override
  public UserInvite getUserInviteByEmailAndAccount(String email, String accountId) {
    UserInvite userInvite = null;
    if (isNotEmpty(email)) {
      userInvite = wingsPersistence.createQuery(UserInvite.class)
                       .filter(UserInviteKeys.email, email)
                       .filter(UserInviteKeys.accountId, accountId)
                       .get();
    }
    return userInvite;
  }

  private void loadUserGroups(String accountId, User user, boolean loadUsers) {
    List<UserGroup> userGroupList = getUserGroupsOfUser(accountId, user.getUuid(), loadUsers);
    user.setUserGroups(userGroupList);
  }

  private void sendVerificationEmail(User user) {
    EmailVerificationToken emailVerificationToken =
        wingsPersistence.saveAndGet(EmailVerificationToken.class, new EmailVerificationToken(user.getUuid()));
    try {
      String verificationUrl =
          buildAbsoluteUrl(configuration.getPortal().getVerificationUrl() + "/" + emailVerificationToken.getToken(),
              user.getDefaultAccountId());

      Map<String, String> templateModel = new HashMap();
      templateModel.put("name", user.getName());
      templateModel.put("url", verificationUrl);
      List<String> toList = new ArrayList();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName(SIGNUP_EMAIL_TEMPLATE_NAME)
                                .templateModel(templateModel)
                                .accountId(getPrimaryAccount(user).getUuid())
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Verification email couldn't be sent", e);
    }
  }

  private Account getPrimaryAccount(User user) {
    return user.getAccounts().get(0);
  }

  private String buildAbsoluteUrl(String fragment, String accountId) throws URISyntaxException {
    String baseUrl = subdomainUrlHelper.getPortalBaseUrl(accountId);
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public void verifyRegisteredOrAllowed(String email) {
    signupService.checkIfEmailIsValid(email);

    final String emailAddress = email.trim();
    User existingUser = getUserByEmail(emailAddress);
    if (existingUser != null && existingUser.isEmailVerified()) {
      throw new UserRegistrationException("User Already Registered", ErrorCode.USER_ALREADY_REGISTERED, USER);
    }
  }

  @Override
  public boolean resendVerificationEmail(String email) {
    User existingUser = getUserByEmail(email);
    if (existingUser == null) {
      throw new UserRegistrationException(EXC_MSG_USER_DOESNT_EXIST, ErrorCode.USER_DOES_NOT_EXIST, USER);
    }

    sendVerificationEmail(existingUser);
    return true;
  }

  @Override
  public boolean resendInvitationEmail(UserService userService, String accountId, String email) {
    logger.info("Initiating resending invitation email for user: {}", email);
    User existingUser = userService.getUserByEmail(email);
    if (existingUser == null) {
      logger.info("Resending invitation email failed. User: {} does not exist.", email);
      throw new UserRegistrationException(EXC_MSG_USER_DOESNT_EXIST, ErrorCode.USER_DOES_NOT_EXIST, USER);
    }
    userService.deleteInvites(accountId, email);
    UserInvite newInvite =
        UserInviteBuilder.anUserInvite().withAccountId(accountId).withAppId(GLOBAL_APP_ID).withEmail(email).build();
    wingsPersistence.save(newInvite);
    userService.sendNewInvitationMail(newInvite, accountService.get(accountId));
    logger.info("Resent invitation email for user: {}", email);
    return true;
  }

  @Override
  public boolean verifyToken(String emailToken) {
    EmailVerificationToken verificationToken = wingsPersistence.createQuery(EmailVerificationToken.class)
                                                   .filter("appId", GLOBAL_APP_ID)
                                                   .filter(EmailVerificationTokenKeys.token, emailToken)
                                                   .get();

    if (verificationToken == null) {
      throw new GeneralException("Email verification token is not found");
    }
    wingsPersistence.updateFields(User.class, verificationToken.getUserId(), ImmutableMap.of("emailVerified", true));
    wingsPersistence.delete(EmailVerificationToken.class, verificationToken.getUuid());
    return true;
  }

  @Override
  public List<UserInvite> inviteUsers(UserInvite userInvite) {
    String accountId = userInvite.getAccountId();

    limitCheck(accountId, userInvite);

    return userInvite.getEmails()
        .stream()
        .map(email -> {
          UserInvite userInviteClone = KryoUtils.clone(userInvite);
          userInviteClone.setEmail(email.trim());
          return inviteUser(userInviteClone);
        })
        .collect(toList());
  }

  private void limitCheck(String accountId, UserInvite userInvite) {
    try {
      Account account = accountService.get(accountId);
      if (null == account) {
        logger.error("No account found for accountId={}", accountId);
        return;
      }

      PageRequest<User> request = aPageRequest()
                                      .addFilter(UserKeys.accounts, IN, Collections.singletonList(account).toArray())
                                      .withLimit(PageRequest.UNLIMITED)
                                      .build();

      List<User> existingUsersAndInvites = list(request, false);
      userServiceLimitChecker.limitCheck(accountId, existingUsersAndInvites, new HashSet<>(userInvite.getEmails()));
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      // catching this because we don't want to stop user invites due to failure in limit check
      logger.error("Error while checking limits. accountId={}", accountId, e);
    }
  }

  @Override
  public UserInvite inviteUser(UserInvite userInvite) {
    // HAR-6861: should validate against invalid email address on user invitation.
    signupService.checkIfEmailIsValid(userInvite.getEmail());

    String accountId = userInvite.getAccountId();
    Account account = accountService.get(accountId);
    String inviteId = wingsPersistence.save(userInvite);
    boolean sendNotification = true;

    if (CollectionUtils.isEmpty(userInvite.getRoles())) {
      Role accountAdminRole = roleService.getAccountAdminRole(accountId);
      if (accountAdminRole != null) {
        List<Role> roleList = new ArrayList<>();
        roleList.add(accountAdminRole);
        userInvite.setRoles(roleList);
      }
    }

    User user = getUserByEmail(userInvite.getEmail());
    if (user == null) {
      AuthenticationMechanism currentAuthenticationMechanism = account.getAuthenticationMechanism();
      boolean emailVerified = currentAuthenticationMechanism != AuthenticationMechanism.USER_PASSWORD;
      user = anUser()
                 .accounts(Lists.newArrayList(account))
                 .email(userInvite.getEmail().trim().toLowerCase())
                 .name(userInvite.getName().trim())
                 .roles(userInvite.getRoles())
                 .appId(GLOBAL_APP_ID)
                 .emailVerified(emailVerified)
                 .imported(userInvite.getImportedByScim())
                 .build();
      user = save(user, accountId);
      // Invitation email should sent only in case of USER_PASSWORD authentication mechanism. Because only in that case
      // we need user to set the password.
      if (currentAuthenticationMechanism == AuthenticationMechanism.USER_PASSWORD) {
        sendNewInvitationMail(userInvite, account);
        sendNotification = false;
      }

      // TODO: PL-2771: Enable the invited User's 2FA if the account is 2FA enabled.
      //      if (account.isTwoFactorAdminEnforced()) {
      //        enableTwoFactorAuthenticationForUser(user);
      //      }
    } else {
      boolean userAlreadyAddedToAccount = user.getAccounts().stream().anyMatch(acc -> acc.getUuid().equals(accountId));
      if (userAlreadyAddedToAccount) {
        addRoles(user, userInvite.getRoles());
      } else {
        addAccountRoles(user, account, userInvite.getRoles());
      }
      if (!accountService.isSSOEnabled(account)) {
        if (!user.isEmailVerified()) {
          sendNewInvitationMail(userInvite, account);
          sendNotification = false;
        }
      }
    }

    List<UserGroup> userGroups = userInvite.getUserGroups();
    if (isNotEmpty(userGroups)) {
      Set<String> userGroupIds = userGroups.stream().map(UserGroup::getUuid).collect(Collectors.toSet());
      PageRequest<UserGroup> pageRequest = aPageRequest()
                                               .addFilter(UserGroup.ACCOUNT_ID_KEY, EQ, accountId)
                                               .addFilter("_id", IN, userGroupIds.toArray())
                                               .build();
      PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
      userGroups = pageResponse.getResponse();
      addUserToUserGroups(accountId, user, userGroups, sendNotification);
    }

    logger.info("Invited user {} to join existing account {} with id {}", userInvite.getEmail(),
        userInvite.getAccountName(), userInvite.getAccountId());

    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, userInvite, Type.CREATE);
    logger.info("Auditing userInvite={} for account={}", userInvite.getEmail(), account.getUuid());

    eventPublishHelper.publishUserInviteFromAccountEvent(accountId, userInvite.getEmail());
    return wingsPersistence.getWithAppId(UserInvite.class, userInvite.getAppId(), inviteId);
  }

  private void addUserToUserGroups(String accountId, User user, List<UserGroup> userGroups, boolean sendNotification) {
    if (isEmpty(userGroups)) {
      return;
    }

    // Stores information about newly added user groups
    Set<String> newUserGroups = Sets.newHashSet();
    for (UserGroup userGroup : userGroups) {
      List<User> userGroupMembers = userGroup.getMembers();
      if (isEmpty(userGroupMembers)) {
        userGroupMembers = new ArrayList<>();
        userGroup.setMembers(userGroupMembers);
      }
      if (!userGroupMembers.contains(user)) {
        userGroupMembers.add(user);
        userGroupService.updateMembers(userGroup, false);
        NotificationSettings notificationSettings = userGroup.getNotificationSettings();
        if (null == notificationSettings) {
          logger.error("Notification settings not found for user group id: [{}]", userGroup.getUuid());
        } else if (notificationSettings.isSendMailToNewMembers()) {
          newUserGroups.add(userGroup.getUuid());
        }
      }
    }

    // Sending email only if user was added to some new group
    if (sendNotification && isNotEmpty(newUserGroups)) {
      sendAddedGroupEmail(user, accountService.get(accountId), userGroups);
    }
  }

  private void addUserToUserGroups(
      String accountId, User user, SetView<String> userGroupIds, boolean sendNotification) {
    if (isNotEmpty(userGroupIds)) {
      List<UserGroup> userGroups = getUserGroups(accountId, userGroupIds);
      addUserToUserGroups(accountId, user, userGroups, sendNotification);
    }
  }

  private void removeUserFromUserGroups(
      String accountId, User user, SetView<String> userGroupIds, boolean sendNotification) {
    if (isNotEmpty(userGroupIds)) {
      List<UserGroup> userGroups = getUserGroups(accountId, userGroupIds);
      removeUserFromUserGroups(user, userGroups, sendNotification);
    }
  }

  private void removeUserFromUserGroups(User user, List<UserGroup> userGroups, boolean sendNotification) {
    if (isNotEmpty(userGroups)) {
      final User userFinal = user;
      userGroups.forEach(userGroup -> {
        List<User> userGroupMembers = userGroup.getMembers();
        if (userGroupMembers != null) {
          userGroupMembers.remove(userFinal);
          userGroupService.updateMembers(userGroup, sendNotification);
        }
      });
    }
  }

  private void removeRelatedUserInvite(String accountId, String email) {
    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class)
                                .filter(UserInviteKeys.email, email)
                                .filter(UserInvite.ACCOUNT_ID_KEY, accountId)
                                .get();
    if (userInvite != null) {
      wingsPersistence.delete(userInvite);
    }
  }

  private List<UserGroup> getUserGroupsOfUser(String accountId, String userId, boolean loadUsers) {
    PageRequest<UserGroup> pageRequest =
        aPageRequest().addFilter(UserGroup.ACCOUNT_ID_KEY, EQ, accountId).addFilter("memberIds", EQ, userId).build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, loadUsers);
    return pageResponse.getResponse();
  }

  private List<UserGroup> getUserGroups(String accountId, SetView<String> userGroupIds) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("_id", IN, userGroupIds.toArray())
                                             .addFilter(UserGroup.ACCOUNT_ID_KEY, EQ, accountId)
                                             .build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    return pageResponse.getResponse();
  }

  private Map<String, String> getNewInvitationTemplateModel(UserInvite userInvite, Account account)
      throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    String inviteUrl = getUserInviteUrl(userInvite, account);
    model.put("company", account.getCompanyName());
    model.put("name", userInvite.getEmail());
    model.put("url", inviteUrl);
    return model;
  }

  @Override
  public String getUserInviteUrl(UserInvite userInvite, Account account) throws URISyntaxException {
    if (userInvite == null) {
      return null;
    }
    return buildAbsoluteUrl(
        format("/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s", account.getUuid(),
            account.getAccountName(), account.getCompanyName(), userInvite.getEmail(), userInvite.getUuid()),
        account.getUuid());
  }

  @Override
  public String getUserInviteUrl(UserInvite userInvite) throws URISyntaxException {
    if (userInvite == null) {
      return null;
    }
    return buildAbsoluteUrl(
        format("/invite?email=%s&inviteId=%s", userInvite.getEmail(), userInvite.getUuid()), userInvite.getAccountId());
  }

  private Map<String, String> getEmailVerificationTemplateModel(String email, String url, String accountId)
      throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    model.put("name", email);
    model.put("url", buildAbsoluteUrl(url, accountId));
    return model;
  }

  private Map<String, String> getEmailVerificationTemplateModel(
      String email, String url, Map<String, String> params, String accountId) {
    Map<String, String> model = new HashMap<>();
    model.put("name", email);
    String baseUrl = subdomainUrlHelper.getPortalBaseUrl(accountId);
    // This uses the setPath. The method above uses setFragment() which adds a # to the url.
    model.put("url", authenticationUtils.buildAbsoluteUrl(baseUrl, url, params).toString());
    return model;
  }

  @Override
  public void sendNewInvitationMail(UserInvite userInvite, Account account) {
    try {
      Map<String, String> templateModel = getNewInvitationTemplateModel(userInvite, account);
      signupService.sendEmail(userInvite, INVITE_EMAIL_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Invitation email couldn't be sent ", e);
    }
  }

  private void sendVerificationEmail(UserInvite userInvite, String url) {
    try {
      Map<String, String> templateModel =
          getEmailVerificationTemplateModel(userInvite.getEmail(), url, userInvite.getAccountId());
      signupService.sendEmail(userInvite, TRIAL_EMAIL_VERIFICATION_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Verification email couldn't be sent ", e);
    }
  }

  @Override
  public void sendVerificationEmail(UserInvite userInvite, String url, Map<String, String> params) {
    Map<String, String> templateModel =
        getEmailVerificationTemplateModel(userInvite.getEmail(), url, params, userInvite.getAccountId());
    signupService.sendEmail(userInvite, TRIAL_EMAIL_VERIFICATION_TEMPLATE_NAME, templateModel);
  }

  private boolean sendEmail(String toEmail, String templateName, Map<String, String> templateModel) {
    List<String> toList = new ArrayList<>();
    toList.add(toEmail);
    EmailData emailData =
        EmailData.builder().to(toList).templateName(templateName).templateModel(templateModel).build();
    emailData.setRetries(2);
    return emailNotificationService.send(emailData);
  }

  private Map<String, Object> getAddedRoleTemplateModel(User user, Account account, List<UserGroup> userGroups)
      throws URISyntaxException {
    List<String> userGroupNamesList = new ArrayList<>();
    userGroups.forEach(userGroup -> { userGroupNamesList.add(userGroup.getName()); });
    String loginUrl = buildAbsoluteUrl(format("/login?company=%s&account=%s&email=%s", account.getCompanyName(),
                                           account.getAccountName(), user.getEmail()),
        account.getUuid());
    Map<String, Object> model = new HashMap<>();
    model.put("name", user.getName());
    model.put("url", loginUrl);
    model.put("company", account.getCompanyName());
    model.put("email", user.getEmail());
    model.put("authenticationMechanism", account.getAuthenticationMechanism().getType());
    model.put("addedToUserGroups", String.join(",", userGroupNamesList));

    // In case of username-password authentication mechanism, we don't need to add the SSO details in the email.
    if (account.getAuthenticationMechanism() == AuthenticationMechanism.USER_PASSWORD) {
      return model;
    }

    SSOSettings ssoSettings;
    if (account.getAuthenticationMechanism() == AuthenticationMechanism.SAML) {
      ssoSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());
    } else if (account.getAuthenticationMechanism() == AuthenticationMechanism.LDAP) {
      ssoSettings = ssoSettingService.getLdapSettingsByAccountId(account.getUuid());
    } else if (account.getAuthenticationMechanism() == AuthenticationMechanism.OAUTH) {
      ssoSettings = ssoSettingService.getOauthSettingsByAccountId(account.getUuid());
    } else {
      logger.warn("New authentication mechanism detected. Needs to handle the added role email template flow.");
      throw new GeneralException("New authentication mechanism detected.");
    }
    model.put("ssoUrl", getDomainName(ssoSettings.getUrl()));
    return model;
  }

  public static String getDomainName(String url) throws URISyntaxException {
    URI uri = new URIBuilder(url).build();
    return uri.getHost();
  }

  @Override
  public void sendAddedGroupEmail(User user, Account account, List<UserGroup> userGroups) {
    try {
      Map<String, Object> templateModel = getAddedRoleTemplateModel(user, account, userGroups);
      List<String> toList = new ArrayList<>();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName(ADD_GROUP_EMAIL_TEMPLATE_NAME)
                                .templateModel(templateModel)
                                .accountId(account.getUuid())
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Add account email couldn't be sent", e);
    }
  }

  @Override
  public PageResponse<UserInvite> listInvites(PageRequest<UserInvite> pageRequest) {
    return wingsPersistence.query(UserInvite.class, pageRequest);
  }

  @Override
  public UserInvite getInvite(String accountId, String inviteId) {
    return wingsPersistence.createQuery(UserInvite.class)
        .filter(UserInvite.ACCOUNT_ID_KEY, accountId)
        .filter(UserInvite.UUID_KEY, inviteId)
        .get();
  }

  private UserInvite getInvite(String inviteId) {
    return wingsPersistence.createQuery(UserInvite.class).filter(UserInvite.UUID_KEY, inviteId).get();
  }

  @Override
  public UserInvite completeInvite(UserInvite userInvite) {
    UserInvite existingInvite = getInvite(userInvite.getAccountId(), userInvite.getUuid());
    if (existingInvite == null) {
      throw new UnauthorizedException(EXC_MSG_USER_INVITE_INVALID, USER);
    }
    if (existingInvite.isCompleted()) {
      return existingInvite;
    }
    if (userInvite.getName() == null || userInvite.getPassword() == null) {
      throw new InvalidRequestException("User name/password is not provided", USER);
    }

    loginSettingsService.verifyPasswordStrength(
        accountService.get(userInvite.getAccountId()), userInvite.getPassword());

    User existingUser = getUserByEmail(existingInvite.getEmail());
    if (existingUser == null) {
      throw new UnauthorizedException(EXC_MSG_USER_INVITE_INVALID, USER);
    } else {
      Map<String, Object> map = new HashMap<>();
      map.put("name", userInvite.getName().trim());
      map.put("passwordHash", hashpw(new String(userInvite.getPassword()), BCrypt.gensalt()));
      map.put("emailVerified", true);
      wingsPersistence.updateFields(User.class, existingUser.getUuid(), map);
    }

    wingsPersistence.updateField(UserInvite.class, existingInvite.getUuid(), "completed", true);
    wingsPersistence.updateField(UserInvite.class, existingInvite.getUuid(), "agreement", userInvite.isAgreement());
    existingInvite.setCompleted(true);

    eventPublishHelper.publishUserRegistrationCompletionEvent(userInvite.getAccountId(), existingUser);
    if (userInvite.getAccountId() != null) {
      auditServiceHelper.reportForAuditingUsingAccountId(
          userInvite.getAccountId(), null, userInvite, Type.ACCEPTED_INVITE);
      logger.info("Auditing accepted invite for userInvite={} in account={}", userInvite.getName(),
          userInvite.getAccountName());
    }
    return existingInvite;
  }

  @Override
  public User completeInviteAndSignIn(UserInvite userInvite) {
    completeInvite(userInvite);
    return authenticationManager.defaultLogin(userInvite.getEmail(), String.valueOf(userInvite.getPassword()));
  }

  @Override
  public User completeTrialSignupAndSignIn(String userInviteId) {
    UserInvite userInvite = getInvite(userInviteId);
    if (userInvite == null) {
      throw new UnauthorizedException(EXC_MSG_USER_INVITE_INVALID, USER);
    }
    return completeTrialSignupAndSignIn(userInvite);
  }

  @Override
  public User completePaidSignupAndSignIn(UserInvite userInvite) {
    String accountName = accountService.suggestAccountName(userInvite.getAccountName());
    String companyName = userInvite.getCompanyName();

    User user = Builder.anUser()
                    .email(userInvite.getEmail())
                    .name(userInvite.getName())
                    .passwordHash(userInvite.getPasswordHash())
                    .accountName(accountName)
                    .companyName(companyName != null ? companyName : accountName)
                    .build();

    completeSignup(user, userInvite, getDefaultPaidLicense());
    return authenticationManager.defaultLoginUsingPasswordHash(userInvite.getEmail(), userInvite.getPasswordHash());
  }

  @Override
  public User completeTrialSignupAndSignIn(UserInvite userInvite) {
    String accountName = accountService.suggestAccountName(userInvite.getAccountName());
    String companyName = userInvite.getCompanyName();

    User user = Builder.anUser()
                    .email(userInvite.getEmail())
                    .name(userInvite.getName())
                    .passwordHash(userInvite.getPasswordHash())
                    .accountName(accountName)
                    .companyName(companyName != null ? companyName : accountName)
                    .utmInfo(userInvite.getUtmInfo())
                    .build();

    completeSignup(user, userInvite, getTrialLicense());

    return authenticationManager.defaultLoginUsingPasswordHash(userInvite.getEmail(), userInvite.getPasswordHash());
  }

  @Override
  public User completeMarketPlaceSignup(User user, UserInvite userInvite, MarketPlaceType marketPlaceType) {
    userInvite = marketPlaceSignup(user, userInvite, marketPlaceType);
    if (null == userInvite.getPassword()) {
      throw new InvalidArgumentsException(Pair.of("args", "Password needs to be specified to login"));
    }
    return authenticationManager.defaultLogin(userInvite.getEmail(), String.valueOf(userInvite.getPassword()));
  }

  private UserInvite marketPlaceSignup(User user, final UserInvite userInvite, MarketPlaceType marketPlaceType) {
    validateUser(user);

    UserInvite existingInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    if (existingInvite == null) {
      throw new UnauthorizedException(EXC_MSG_USER_INVITE_INVALID, USER);
    } else if (existingInvite.isCompleted()) {
      logger.error("Unexpected state: Existing invite is already completed. ID={}, userInvite: {} existingInvite: {}",
          userInvite.getUuid(), userInvite, existingInvite);

      // password is marked transient, so won't be saved in existingInvite
      existingInvite.setPassword(userInvite.getPassword());
      return userInvite;
    }

    String email = user.getEmail();
    User existingUser = getUserByEmail(email);
    if (existingUser != null) {
      throw new UserRegistrationException(EXC_USER_ALREADY_REGISTERED, ErrorCode.USER_ALREADY_REGISTERED, USER);
    }

    if (userInvite.getMarketPlaceToken() == null) {
      throw new GeneralException(
          String.format("Marketplace token not found for userInviteID:[{%s}]", userInvite.getUuid()));
    }

    String userInviteID;
    String marketPlaceID;
    try {
      Map<String, Claim> claims =
          secretManager.verifyJWTToken(userInvite.getMarketPlaceToken(), JWT_CATEGORY.MARKETPLACE_SIGNUP);
      userInviteID = claims.get(MarketPlaceConstants.USERINVITE_ID_CLAIM_KEY).asString();
      marketPlaceID = claims.get(MarketPlaceConstants.MARKETPLACE_ID_CLAIM_KEY).asString();
      if (!userInviteID.equals(userInvite.getUuid())) {
        throw new GeneralException(String.format(
            "UserInviteID in claim:[{%s}] does not match the userInviteID:[{%s}]", userInviteID, userInvite.getUuid()));
      }
    } catch (Exception e) {
      throw new HintException("Invalid Marketplace token: " + e.getMessage());
    }
    MarketPlace marketPlace = wingsPersistence.get(MarketPlace.class, marketPlaceID);

    if (marketPlace == null) {
      throw new GeneralException(String.format("No MarketPlace found with marketPlaceID=[{%s}]", marketPlaceID));
    }

    LicenseInfo licenseInfo = LicenseInfo.builder()
                                  .accountType(AccountType.PAID)
                                  .licenseUnits(marketPlace.getOrderQuantity())
                                  .expiryTime(marketPlace.getExpirationDate().getTime())
                                  .accountStatus(AccountStatus.ACTIVE)
                                  .build();

    Account account = Account.Builder.anAccount()
                          .withAccountName(user.getAccountName())
                          .withCompanyName(user.getCompanyName())
                          .withLicenseInfo(licenseInfo)
                          .withAppId(GLOBAL_APP_ID)
                          .build();

    account = setupAccount(account);
    String accountId = account.getUuid();
    List<UserGroup> accountAdminGroups = getAccountAdminGroup(accountId);
    completeUserInviteForSignup(userInvite, accountId);

    marketPlace.setAccountId(accountId);
    wingsPersistence.save(marketPlace);
    saveUserAndUserGroups(user, email, account, accountAdminGroups);

    boolean isGcpMarketPlace = MarketPlaceType.GCP == marketPlaceType;
    if (isGcpMarketPlace) {
      // 1. Create billing scheduler entry
      final Instant nextIteration = Instant.now().plus(1, ChronoUnit.HOURS);
      gcpBillingPollingService.create(new GCPBillingJobEntity(
          accountId, marketPlace.getCustomerIdentificationCode(), nextIteration.toEpochMilli()));

      // 2. approve call to GCP
      gcpProcurementService.approve(marketPlace);
      gcpProcurementService.approveRequestedEntitlement(marketPlace);
    }

    return userInvite;
  }

  private void saveUserAndUserGroups(User user, String email, Account account, List<UserGroup> accountAdminGroups) {
    user.setAppId(GLOBAL_APP_ID);
    user.setEmail(user.getEmail().trim().toLowerCase());
    if (isEmpty(user.getPasswordHash())) {
      user.setPasswordHash(hashpw(new String(user.getPassword()), BCrypt.gensalt()));
    }
    user.setEmailVerified(true);
    user.getAccounts().add(account);
    user.setUserGroups(accountAdminGroups);

    save(user, account.getUuid());

    addUserToUserGroups(account.getUuid(), user, accountAdminGroups, false);
  }

  private void validateUser(User user) {
    if (user.getAccountName() == null || user.getCompanyName() == null) {
      throw new InvalidRequestException("Account/company name is not provided", USER);
    }
    if (isEmpty(user.getName()) || isEmpty(user.getPassword())) {
      throw new InvalidRequestException("User's name/password is not provided", USER);
    }
  }

  @Override
  public UserInvite completeSignup(User user, UserInvite userInvite, LicenseInfo licenseInfo) {
    if (user.getAccountName() == null || user.getCompanyName() == null) {
      throw new InvalidRequestException("Account/company name is not provided", USER);
    }
    if (isEmpty(user.getName()) || (isEmpty(user.getPassword()) && isEmpty(user.getPasswordHash()))) {
      throw new InvalidRequestException("User's name/password is not provided", USER);
    }

    UserInvite existingInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    if (existingInvite == null) {
      throw new UnauthorizedException(EXC_MSG_USER_INVITE_INVALID, USER);
    } else if (existingInvite.isCompleted()) {
      return existingInvite;
    }

    String email = existingInvite.getEmail();
    User existingUser = getUserByEmail(email);
    if (existingUser != null) {
      throw new UserRegistrationException(EXC_USER_ALREADY_REGISTERED, ErrorCode.USER_ALREADY_REGISTERED, USER);
    }

    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    Account account = Account.Builder.anAccount()
                          .withAccountName(user.getAccountName())
                          .withCompanyName(user.getCompanyName())
                          .withAppId(GLOBAL_APP_ID)
                          .withLicenseInfo(licenseInfo)
                          .build();
    // Create an trial account which license expires in 15 days.
    account = setupAccount(account);
    String accountId = account.getUuid();

    // For trial user just signed up, it will be assigned to the account admin role.
    List<UserGroup> accountAdminGroups = getAccountAdminGroup(accountId);

    completeUserInviteForSignup(userInvite, accountId);

    saveUserAndUserGroups(user, email, account, accountAdminGroups);

    eventPublishHelper.publishUserRegistrationCompletionEvent(accountId, user);
    if (userInvite.getAccountId() != null) {
      auditServiceHelper.reportForAuditingUsingAccountId(
          userInvite.getAccountId(), null, userInvite, Type.ACCEPTED_INVITE);
      logger.info(
          "Auditing accepted invite for userInvite={} in account={}", userInvite.getName(), account.getAccountName());
    }
    return userInvite;
  }

  private LicenseInfo getTrialLicense() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    return licenseInfo;
  }

  private LicenseInfo getDefaultPaidLicense() {
    final Instant defaultExpiry = Instant.now().plus(365, ChronoUnit.DAYS);
    return LicenseInfo.builder()
        .accountType(AccountType.PAID)
        .licenseUnits(50)
        .expiryTime(from(defaultExpiry).getTime())
        .accountStatus(AccountStatus.ACTIVE)
        .build();
  }

  private void completeUserInviteForSignup(UserInvite userInvite, String accountId) {
    userInvite.setAccountId(accountId);
    userInvite.setCompleted(true);
    userInvite.setAgreement(true);

    // Update existing invite with the associated account ID and set the status to be completed.
    wingsPersistence.save(userInvite);
  }

  @Override
  public User signUpUserUsingOauth(OauthUserInfo userInfo, String oauthProviderName) {
    logger.info("User not found in db. Creating an account for: [{}]", userInfo.getEmail());
    checkForFreemiumCluster();
    User user = createUser(userInfo, oauthProviderName);
    notNullOrEmptyCheck(user.getAccountName(), "Account/Company name");
    notNullOrEmptyCheck(user.getName(), "User's name");

    throwExceptionIfUserIsAlreadyRegistered(user.getEmail());

    // Create a trial account whose license expires in 15 days.
    Account account = createAccountWithTrialLicense(user);

    // For trial user just signed up, it will be assigned to the account admin role.
    user = assignUserToAccountAdminGroup(user, account);

    String accountId = account.getUuid();
    createSSOSettingsAndMarkAsDefaultAuthMechanism(accountId);

    // PL-2698: UI lead-update call will be called only if it's first login. Will need to
    // make sure the isFirstLogin is always derived from lastLogin value.
    boolean isFirstLogin = user.getLastLogin() == 0L;
    user.setFirstLogin(isFirstLogin);

    return user;
  }

  private void checkForFreemiumCluster() {
    // A safe check to ensure that no signup is allowed in the paid cluster.
    if (!configuration.isTrialRegistrationAllowed()) {
      throw new InvalidRequestException("Signup is not allowed in paid cluster");
    }
  }

  private User createUser(OauthUserInfo userInfo, String oauthProvider) {
    String email = userInfo.getEmail();
    final String companyName = accountService.suggestAccountName(userInfo.getName());
    return Builder.anUser()
        .email(email)
        .name(userInfo.getName())
        .accountName(companyName)
        .companyName(companyName)
        .emailVerified(true)
        .oauthProvider(oauthProvider)
        .utmInfo(userInfo.getUtmInfo())
        .build();
  }

  private void notNullOrEmptyCheck(String name, String errorSubject) {
    if (Strings.isNullOrEmpty(name)) {
      throw new InvalidRequestException(errorSubject + " is empty", USER);
    }
  }

  private User assignUserToAccountAdminGroup(User user, Account account) {
    List<UserGroup> accountAdminGroups = getAccountAdminGroup(account.getUuid());

    user.setAppId(GLOBAL_APP_ID);
    user.getAccounts().add(account);
    user.setUserGroups(accountAdminGroups);
    user = save(user, account.getUuid());

    addUserToUserGroups(account.getUuid(), user, accountAdminGroups, false);

    return user;
  }

  private void throwExceptionIfUserIsAlreadyRegistered(final String email) {
    User existingUser = getUserByEmail(email);
    if (existingUser != null) {
      throw new UserRegistrationException(EXC_USER_ALREADY_REGISTERED, ErrorCode.USER_ALREADY_REGISTERED, USER);
    }
  }

  private Account createAccountWithTrialLicense(User user) {
    LicenseInfo licenseInfo = getTrialLicense();
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    Account account = Account.Builder.anAccount()
                          .withAccountName(user.getAccountName())
                          .withCompanyName(user.getCompanyName())
                          .withAppId(GLOBAL_APP_ID)
                          .withLicenseInfo(licenseInfo)
                          .build();

    account = setupAccount(account);
    return account;
  }

  private void createSSOSettingsAndMarkAsDefaultAuthMechanism(String accountId) {
    OauthSettings oauthSettings =
        OauthSettings.builder()
            .accountId(accountId)
            .displayName(
                Arrays.stream(OauthProviderType.values()).map(OauthProviderType::name).collect(Collectors.joining(",")))
            .allowedProviders(Arrays.stream(OauthProviderType.values()).collect(Collectors.toSet()))
            .build();
    ssoSettingService.saveOauthSettings(oauthSettings);
    logger.info("Setting authentication mechanism as oauth for account id: {}", accountId);
    ssoService.setAuthenticationMechanism(accountId, AuthenticationMechanism.OAUTH);
  }

  @Override
  public UserInvite deleteInvite(String accountId, String inviteId) {
    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class)
                                .filter(ID_KEY, inviteId)
                                .filter(UserInvite.ACCOUNT_ID_KEY, accountId)
                                .get();
    if (userInvite != null) {
      wingsPersistence.delete(userInvite);
    }
    return userInvite;
  }

  @Override
  public boolean deleteInvites(String accountId, String email) {
    Query userInvitesQuery = wingsPersistence.createQuery(UserInvite.class)
                                 .filter(UserInvite.ACCOUNT_ID_KEY, accountId)
                                 .filter(UserInviteKeys.email, email);
    return wingsPersistence.delete(userInvitesQuery);
  }

  @Override
  public boolean resetPassword(String email) {
    User user = getUserByEmail(email);

    if (user == null) {
      throw new InvalidRequestException("Email doesn't exist", USER);
    }

    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      String token = JWT.create()
                         .withIssuer("Harness Inc")
                         .withIssuedAt(new Date())
                         .withExpiresAt(new Date(System.currentTimeMillis() + 4 * 60 * 60 * 1000)) // 4 hrs
                         .withClaim("email", email)
                         .sign(algorithm);
      sendResetPasswordEmail(user, token);
      if (isNotEmpty(user.getAccounts())) {
        user.getAccounts().forEach(account -> {
          auditServiceHelper.reportForAuditingUsingAccountId(account.getUuid(), null, user, Type.RESET_PASSWORD);
          logger.info(
              "Auditing resetting passowrd for user={} in account={}", user.getName(), account.getAccountName());
        });
      }
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new GeneralException(EXC_MSG_RESET_PASS_LINK_NOT_GEN);
    }
    return true;
  }

  @Override
  public boolean updatePassword(String resetPasswordToken, char[] password) {
    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(resetPasswordToken);
      JWT decode = JWT.decode(resetPasswordToken);
      String email = decode.getClaim("email").asString();
      resetUserPassword(email, password, decode.getIssuedAt().getTime());
    } catch (UnsupportedEncodingException exception) {
      throw new GeneralException("Invalid reset password link");
    } catch (JWTVerificationException exception) {
      throw new UnauthorizedException("Token has expired", USER);
    }
    return true;
  }

  @Override
  public void logout(User user) {
    authService.invalidateToken(user.getToken());
    evictUserFromCache(user.getUuid());
  }

  private void resetUserPassword(String email, char[] password, long tokenIssuedAt) {
    User user = getUserByEmail(email);
    if (user == null) {
      throw new InvalidRequestException("Email doesn't exist");
    } else if (user.getPasswordChangedAt() > tokenIssuedAt) {
      throw new UnauthorizedException("Token has expired", USER);
    }
    loginSettingsService.verifyPasswordStrength(accountService.get(user.getDefaultAccountId()), password);
    String hashed = hashpw(new String(password), BCrypt.gensalt());
    wingsPersistence.update(user,
        wingsPersistence.createUpdateOperations(User.class)
            .set("passwordHash", hashed)
            .set("passwordExpired", false)
            .set("passwordChangedAt", System.currentTimeMillis()));
    executorService.submit(() -> authService.invalidateAllTokensForUser(user.getUuid()));
  }

  private Account getHarnessIoAccountForUser(List<Account> accounts) {
    Account harnessAccount = null;
    for (Account account : accounts) {
      if (account.getAccountName().equals("Harness.io")) {
        harnessAccount = account;
      }
    }
    return harnessAccount;
  }

  @Override
  public boolean isTwoFactorEnabled(String accountId, String userId) {
    // Check if admin has 2FA enabled
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new UnauthorizedException(EXC_MSG_USER_DOESNT_EXIST, USER);
    }
    return user.isTwoFactorAuthenticationEnabled();
  }

  @Override
  public boolean overrideTwoFactorforAccount(String accountId, boolean adminOverrideTwoFactorEnabled) {
    try {
      Query<User> updateQuery = wingsPersistence.createQuery(User.class);
      updateQuery.filter(UserKeys.accounts, accountId);
      if (updateQuery.count() > 0) {
        for (User u : updateQuery) {
          // Look for user who has only 1 account
          if (u.getAccounts().size() == 1) {
            if (!u.isTwoFactorAuthenticationEnabled()) {
              enableTwoFactorAuthenticationForUser(u);
            }
          }
        }
      }
    } catch (Exception ex) {
      throw new GeneralException("Exception occurred while enforcing Two factor authentication for users");
    }

    return true;
  }

  private void enableTwoFactorAuthenticationForUser(User user) {
    user.setTwoFactorAuthenticationEnabled(true);
    user.setTwoFactorAuthenticationMechanism(TwoFactorAuthenticationMechanism.TOTP);
    update(user);
    twoFactorAuthenticationManager.sendTwoFactorAuthenticationResetEmail(user.getUuid());
  }

  private void sendResetPasswordEmail(User user, String token) {
    try {
      String resetPasswordUrl = getResetPasswordUrl(token, user);

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("name", user.getName());
      templateModel.put("url", resetPasswordUrl);
      List<String> toList = new ArrayList();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName("reset_password")
                                .templateModel(templateModel)
                                .accountId(getPrimaryAccount(user).getUuid())
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Reset password email couldn't be sent", e);
    }
  }

  private String getResetPasswordUrl(String token, User user) throws URISyntaxException {
    String accountIdParam = "?accountId=" + user.getDefaultAccountId();
    return buildAbsoluteUrl("/reset-password/" + token + accountIdParam, user.getDefaultAccountId());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#matchPassword(java.lang.String, java.lang.String)
   */
  @Override
  public boolean matchPassword(char[] password, String hash) {
    return BCrypt.checkpw(new String(password), hash);
  }

  @Override
  public User save(User user, String accountId) {
    user = wingsPersistence.saveAndGet(User.class, user);
    evictUserFromCache(user.getUuid());
    eventPublishHelper.publishSetupRbacEvent(accountId, user.getUuid(), EntityType.USER);
    return user;
  }

  @Override
  public User updateUserProfile(@NotNull User user) {
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    if (user.getName() != null) {
      updateOperations.set(UserKeys.name, user.getName());
    } else {
      updateOperations.unset(UserKeys.name);
    }
    if (isNotEmpty(user.getAccounts())) {
      user.getAccounts().forEach(account -> {
        auditServiceHelper.reportForAuditingUsingAccountId(account.getUuid(), null, user, Event.Type.UPDATE);
        logger.info(
            "Auditing update of User Profile for user={} in account={}", user.getUuid(), account.getAccountName());
      });
    }
    return applyUpdateOperations(user, updateOperations);
  }

  @Override
  public User addEventToUserMarketoCampaigns(String userId, EventType eventType) {
    User user = this.getUserFromCacheOrDB(userId);
    Set<String> consolidatedReportedMarketoCampaigns = Sets.newHashSet();
    Set<String> reportedMarketoCampaigns = user.getReportedMarketoCampaigns();
    if (isNotEmpty(reportedMarketoCampaigns)) {
      consolidatedReportedMarketoCampaigns.addAll(reportedMarketoCampaigns);
    }

    consolidatedReportedMarketoCampaigns.add(eventType.name());
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set(UserKeys.reportedMarketoCampaigns, consolidatedReportedMarketoCampaigns);

    return applyUpdateOperations(user, updateOperations);
  }

  @Override
  public User updateTwoFactorAuthenticationSettings(User user, TwoFactorAuthenticationSettings settings) {
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    updateOperations.set(UserKeys.twoFactorAuthenticationEnabled, settings.isTwoFactorAuthenticationEnabled());
    addTwoFactorAuthenticationOperation(settings.getMechanism(), updateOperations);
    addTotpSecretKeyOperation(settings.getTotpSecretKey(), updateOperations);
    return this.applyUpdateOperations(user, updateOperations);
  }

  private void addTwoFactorAuthenticationOperation(
      @NotNull TwoFactorAuthenticationMechanism twoFactorAuthenticationMechanism,
      @NotNull UpdateOperations<User> updateOperations) {
    if (twoFactorAuthenticationMechanism != null) {
      updateOperations.set(UserKeys.twoFactorAuthenticationMechanism, twoFactorAuthenticationMechanism);
    } else {
      updateOperations.unset(UserKeys.twoFactorAuthenticationMechanism);
    }
  }

  private void addTotpSecretKeyOperation(
      @NotNull String totpSecretKey, @NotNull UpdateOperations<User> updateOperations) {
    if (totpSecretKey != null) {
      updateOperations.set(UserKeys.totpSecretKey, totpSecretKey);
    } else {
      updateOperations.unset(UserKeys.totpSecretKey);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#update(software.wings.beans.User)
   */
  @Override
  public User update(User user) {
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);

    if (user.getPassword() != null && user.getPassword().length > 0) {
      updateOperations.set("passwordHash", hashpw(new String(user.getPassword()), BCrypt.gensalt()));
      updateOperations.set("passwordChangedAt", System.currentTimeMillis());
    }
    if (isNotEmpty(user.getRoles())) {
      updateOperations.set("roles", user.getRoles());
    }

    if (user.getName() != null) {
      updateOperations.set("name", user.getName());
    } else {
      updateOperations.unset("name");
    }

    updateOperations.set("twoFactorAuthenticationEnabled", user.isTwoFactorAuthenticationEnabled());
    addTwoFactorAuthenticationOperation(user.getTwoFactorAuthenticationMechanism(), updateOperations);
    addTotpSecretKeyOperation(user.getTotpSecretKey(), updateOperations);

    if (user.getMarketoLeadId() > 0) {
      updateOperations.set("marketoLeadId", user.getMarketoLeadId());
    }

    if (isNotEmpty(user.getReportedMarketoCampaigns())) {
      updateOperations.set("reportedMarketoCampaigns", user.getReportedMarketoCampaigns());
    }

    if (isNotEmpty(user.getSegmentIdentity())) {
      updateOperations.set("segmentIdentity", user.getSegmentIdentity());
    }

    if (isNotEmpty(user.getReportedSegmentTracks())) {
      updateOperations.set("reportedSegmentTracks", user.getReportedSegmentTracks());
    }

    if (user.getLastLogin() > 0L) {
      updateOperations.set("lastLogin", user.getLastLogin());
    }

    return applyUpdateOperations(user, updateOperations);
  }

  @Override
  public User unlockUser(String email, String accountId) {
    User user = getUserByEmail(email);
    Preconditions.checkNotNull(user, "User cannot be null");
    Preconditions.checkNotNull(user.getDefaultAccountId(), "Default account can't be null for any user");
    if (!user.getDefaultAccountId().equals(accountId)) {
      throw new UnauthorizedException("User not authorized", USER);
    }
    UpdateOperations<User> operations = wingsPersistence.createUpdateOperations(User.class);
    setUnset(operations, UserKeys.userLocked, false);
    setUnset(operations, UserKeys.userLockoutInfo, new UserLockoutInfo());
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, user, Type.UNLOCK);
    logger.info("Auditing unlocking of user={} in account={}", user.getName(), accountId);
    return applyUpdateOperations(user, operations);
  }

  @Override
  public User applyUpdateOperations(User user, UpdateOperations<User> updateOperations) {
    wingsPersistence.update(user, updateOperations);
    evictUserFromCache(user.getUuid());
    return wingsPersistence.getWithAppId(User.class, user.getAppId(), user.getUuid());
  }

  @Override
  public User updateUserGroupsOfUser(
      String userId, List<UserGroup> userGroups, String accountId, boolean sendNotification) {
    User userFromDB = get(accountId, userId);
    List<UserGroup> oldUserGroups = userFromDB.getUserGroups();

    Set<String> oldUserGroupIds = oldUserGroups.stream().map(UserGroup::getUuid).collect(Collectors.toSet());
    Set<String> newUserGroupIds = userGroups.stream().map(UserGroup::getUuid).collect(Collectors.toSet());

    SetView<String> userGroupMemberDeletions = Sets.difference(oldUserGroupIds, newUserGroupIds);
    SetView<String> userGroupMemberAdditions = Sets.difference(newUserGroupIds, oldUserGroupIds);

    if (isNotEmpty(userGroupMemberAdditions)) {
      addUserToUserGroups(accountId, userFromDB, userGroupMemberAdditions, sendNotification);
    }

    if (isNotEmpty(userGroupMemberDeletions)) {
      removeUserFromUserGroups(accountId, userFromDB, userGroupMemberDeletions, false);
    }

    authService.evictUserPermissionAndRestrictionCacheForAccount(accountId, Arrays.asList(userId));
    return get(accountId, userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<User> list(PageRequest<User> pageRequest, boolean loadUserGroups) {
    String accountId = null;
    SearchFilter searchFilter = pageRequest.getFilters()
                                    .stream()
                                    .filter(filter -> filter.getFieldName().equals(UserKeys.accounts))
                                    .findFirst()
                                    .orElse(null);

    if (searchFilter != null) {
      Account account = (Account) searchFilter.getFieldValues()[0];
      accountId = account.getUuid();
    }

    PageResponse<User> pageResponse = wingsPersistence.query(User.class, pageRequest);
    if (loadUserGroups) {
      loadUserGroupsForUsers(pageResponse.getResponse(), accountId);
    }
    return pageResponse;
  }

  private void loadUserGroupsForUsers(List<User> users, String accountId) {
    PageRequest<UserGroup> req = aPageRequest().addFilter("accountId", Operator.EQ, accountId).build();
    PageResponse<UserGroup> res = userGroupService.list(accountId, req, false);
    List<UserGroup> allUserGroupList = res.getResponse();
    if (isEmpty(allUserGroupList)) {
      return;
    }

    Multimap<String, UserGroup> userUserGroupMap = HashMultimap.create();

    allUserGroupList.forEach(userGroup -> {
      List<String> memberIds = userGroup.getMemberIds();
      if (isEmpty(memberIds)) {
        return;
      }
      memberIds.forEach(userId -> userUserGroupMap.put(userId, userGroup));
    });

    users.forEach(user -> {
      Collection<UserGroup> userGroups = userUserGroupMap.get(user.getUuid());
      if (isEmpty(userGroups)) {
        user.setUserGroups(new ArrayList<>());
      } else {
        user.setUserGroups(new ArrayList<>(userGroups));
      }
    });
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#delete(java.lang.String)
   */
  @Override
  public void delete(String accountId, String userId) {
    User user = get(userId);
    if (user.getAccounts() == null) {
      return;
    }

    // HAR-7189: If user removed, the corresponding user invite using the same email address should be removed.
    removeRelatedUserInvite(accountId, user.getEmail());

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new io.harness.limits.Action(accountId, ActionType.CREATE_USER));

    LimitEnforcementUtils.withCounterDecrement(checker, () -> {
      for (Account account : user.getAccounts()) {
        if (account.getUuid().equals(accountId)) {
          user.getAccounts().remove(account);
          break;
        }
      }

      if (user.getAccounts().isEmpty() && wingsPersistence.delete(User.class, userId)) {
        evictUserFromCache(userId);
        return;
      }

      List<Role> accountRoles = roleService.getAccountRoles(accountId);
      if (accountRoles != null) {
        for (Role role : accountRoles) {
          user.getRoles().remove(role);
        }
      }

      PageResponse<UserGroup> pageResponse =
          userGroupService.list(accountId, aPageRequest().addFilter("memberIds", HAS, user.getUuid()).build(), true);
      List<UserGroup> userGroupList = pageResponse.getResponse();
      userGroupList.forEach(userGroup -> {
        List<User> members = userGroup.getMembers();
        if (isNotEmpty(members)) {
          // Find the user to be removed, then remove from the member list and update the user group.
          Optional<User> userOptional = members.stream().filter(member -> member.getUuid().equals(userId)).findFirst();
          if (userOptional.isPresent()) {
            members.remove(userOptional.get());
            userGroupService.updateMembers(userGroup, false);
          }
        }
      });

      UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class)
                                            .set("roles", user.getRoles())
                                            .set(UserKeys.accounts, user.getAccounts());
      Query<User> updateQuery = wingsPersistence.createQuery(User.class).filter(ID_KEY, userId);
      wingsPersistence.update(updateQuery, updateOp);

      removeUserFromUserGroups(user, user.getUserGroups(), false);

      evictUserFromCache(userId);
    });
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, user);
    logger.info("Auditing deletion of user={} in account={}", user.getName(), accountId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#get(java.lang.String)
   */
  @Override
  public User get(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new UnauthorizedException(EXC_MSG_USER_DOESNT_EXIST, USER);
    }

    loadSupportAccounts(user);

    List<Account> accounts = user.getAccounts();
    if (isNotEmpty(accounts)) {
      accounts.forEach(account -> licenseService.decryptLicenseInfo(account, false));
    }

    return user;
  }

  private void loadSupportAccounts(User user) {
    if (user == null) {
      return;
    }

    if (harnessUserGroupService.isHarnessSupportUser(user.getUuid())) {
      Set<String> excludeAccounts = user.getAccounts().stream().map(Account::getUuid).collect(Collectors.toSet());
      List<Account> accountList =
          harnessUserGroupService.listAllowedSupportAccountsForUser(user.getUuid(), excludeAccounts);
      user.setSupportAccounts(accountList);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#get(java.lang.String, java.lang.String)
   */
  @Override
  public User get(String accountId, String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new UnauthorizedException(EXC_MSG_USER_DOESNT_EXIST, USER);
    }

    loadSupportAccounts(user);
    loadUserGroups(accountId, user, false);
    return user;
  }

  @Override
  public User getUserFromCacheOrDB(String userId) {
    Cache<String, User> userCache = cacheManager.getUserCache();
    User user = null;
    try {
      if (userCache == null) {
        return get(userId);
      }
      user = userCache.get(userId);

      if (user == null) {
        logger.info("User [{}] not found in Cache. Load it from DB", userId);
        user = get(userId);
        userCache.put(user.getUuid(), user);
      }
      return user;
    } catch (Exception ex) {
      // If there was any exception, remove that entry from cache
      if (userCache != null) {
        userCache.remove(userId);
        user = get(userId);
        userCache.put(user.getUuid(), user);
      }
    }

    return user;
  }

  @Override
  public void evictUserFromCache(String userId) {
    Preconditions.checkNotNull(cacheManager.getUserCache(), "User cache can't be null");
    cacheManager.getUserCache().remove(userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#addRole(java.lang.String, java.lang.String)
   */
  @Override
  public User addRole(String userId, String roleId) {
    ensureUserExists(userId);
    Role role = ensureRolePresent(roleId);

    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).addToSet("roles", role);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).filter(ID_KEY, userId);
    wingsPersistence.update(updateQuery, updateOp);
    evictUserFromCache(userId);
    return wingsPersistence.get(User.class, userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#revokeRole(java.lang.String, java.lang.String)
   */
  @Override
  public User revokeRole(String userId, String roleId) {
    ensureUserExists(userId);
    Role role = ensureRolePresent(roleId);

    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).removeAll("roles", role);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).filter(ID_KEY, userId);
    wingsPersistence.update(updateQuery, updateOp);
    evictUserFromCache(userId);
    return wingsPersistence.get(User.class, userId);
  }

  @Override
  public AccountRole getUserAccountRole(String userId, String accountId) {
    Account account = accountService.get(accountId);
    if (account == null) {
      String message = "Account [" + accountId + "] does not exist";
      logger.warn(message);
      throw new GeneralException(message);
    }
    User user = get(userId);

    if (user.isAccountAdmin(accountId)) {
      ImmutableList.Builder<ImmutablePair<ResourceType, Action>> builder = ImmutableList.builder();
      for (ResourceType resourceType : ResourceType.values()) {
        for (Action action : Action.values()) {
          builder.add(ImmutablePair.of(resourceType, action));
        }
      }
      return anAccountRole()
          .withAccountId(accountId)
          .withAccountName(account.getAccountName())
          .withAllApps(true)
          .withResourceAccess(builder.build())
          .build();

    } else if (user.isAllAppAdmin(accountId)) {
      ImmutableList.Builder<ImmutablePair<ResourceType, Action>> builder = ImmutableList.builder();
      for (ResourceType resourceType : asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
        for (Action action : Action.values()) {
          builder.add(ImmutablePair.of(resourceType, action));
        }
      }
      return anAccountRole()
          .withAccountId(accountId)
          .withAccountName(account.getAccountName())
          .withAllApps(true)
          .withResourceAccess(builder.build())
          .build();
    }
    return null;
  }

  @Override
  public ApplicationRole getUserApplicationRole(String userId, String appId) {
    Application application = appService.get(appId);
    User user = get(userId);
    if (user.isAccountAdmin(application.getAccountId()) || user.isAppAdmin(application.getAccountId(), appId)) {
      ImmutableList.Builder<ImmutablePair<ResourceType, Action>> builder = ImmutableList.builder();
      for (ResourceType resourceType : asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
        for (Action action : Action.values()) {
          builder.add(ImmutablePair.of(resourceType, action));
        }
      }
      return anApplicationRole()
          .withAppId(appId)
          .withAppName(application.getName())
          .withAllEnvironments(true)
          .withResourceAccess(builder.build())
          .build();
    }
    // TODO - for Prod support and non prod support
    return null;
  }

  @Override
  public ZendeskSsoLoginResponse generateZendeskSsoJwt(String returnToUrl) {
    String jwtZendeskSecret = configuration.getPortal().getJwtZendeskSecret();
    if (jwtZendeskSecret == null) {
      throw new InvalidRequestException("Request can not be completed. No Zendesk SSO secret found.");
    }

    User user = UserThreadLocal.get();

    // Given a user instance
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issueTime(new Date())
                                 .jwtID(UUID.randomUUID().toString())
                                 .claim("name", user.getName())
                                 .claim("email", user.getEmail())
                                 .build();

    // Create JWS header with HS256 algorithm
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).contentType("text/plain").build();

    // Create JWS object
    JWSObject jwsObject = new JWSObject(header, new Payload(jwtClaims.toJSONObject()));

    try {
      // Create HMAC signer
      JWSSigner signer = new MACSigner(jwtZendeskSecret.getBytes(UTF_8));
      jwsObject.sign(signer);
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new InvalidRequestException("Error signing JWT: " + ExceptionUtils.getMessage(e));
    }

    // Serialise to JWT compact form
    String jwtString = jwsObject.serialize();

    String redirectUrl = "https://"
        + "harnesssupport.zendesk.com/access/jwt?jwt=" + jwtString;

    if (returnToUrl != null) {
      redirectUrl += "&return_to=" + returnToUrl;
    }
    return ZendeskSsoLoginResponse.builder().redirectUrl(redirectUrl).userId(user.getUuid()).build();
  }

  private Role ensureRolePresent(String roleId) {
    Role role = roleService.get(roleId);
    if (role == null) {
      throw new GeneralException("Roles does not exist");
    }
    return role;
  }

  private void ensureUserExists(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new UnauthorizedException(EXC_MSG_USER_DOESNT_EXIST, USER);
    }
  }

  private void addAccountAdminRole(User existingUser, Account account) {
    addAccountRoles(existingUser, account, Lists.newArrayList(roleService.getAccountAdminRole(account.getUuid())));
  }

  private void addAccountRoles(User existingUser, Account account, List<Role> roles) {
    UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(User.class);
    if (isNotEmpty(roles)) {
      updateOperations.addToSet("roles", roles);
    }
    if (account != null) {
      updateOperations.addToSet(UserKeys.accounts, account);
    }
    wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                .filter(UserKeys.email, existingUser.getEmail())
                                .filter("appId", existingUser.getAppId()),
        updateOperations);
  }

  private void addRoles(User user, List<Role> roles) {
    if (isNotEmpty(roles)) {
      UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(User.class);
      updateOperations.addToSet("roles", roles);
      wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                  .filter(UserKeys.email, user.getEmail())
                                  .filter("appId", user.getAppId()),
          updateOperations);
    }
  }

  private Account setupTrialAccount(String accountName, String companyName) {
    Account account = Account.Builder.anAccount()
                          .withAccountName(accountName)
                          .withCompanyName(companyName)
                          .withLicenseInfo(LicenseInfo.builder().accountType(AccountType.TRIAL).build())
                          .build();
    return setupAccount(account);
  }

  private Account setupAccount(Account account) {
    // HAR-8645: Always set default appId for account creation to pass validation
    account.setAppId(GLOBAL_APP_ID);
    Account savedAccount = accountService.save(account, false);
    logger.info("New account created with accountId {} and licenseType {}", account.getUuid(),
        account.getLicenseInfo().getAccountType());
    return savedAccount;
  }

  private List<UserGroup> getAccountAdminGroup(String accountId) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter(UserGroup.ACCOUNT_ID_KEY, EQ, accountId)
                                             .addFilter("name", EQ, UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME)
                                             .build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    return pageResponse.getResponse();
  }

  @Override
  public String generateJWTToken(Map<String, String> claims, SecretManager.JWT_CATEGORY category) {
    String jwtPasswordSecret = secretManager.getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTCreator.Builder jwtBuilder =
          JWT.create()
              .withIssuer("Harness Inc")
              .withIssuedAt(new Date())
              .withExpiresAt(new Date(System.currentTimeMillis() + category.getValidityDuration()));
      if (claims != null && claims.size() > 0) {
        claims.forEach(jwtBuilder::withClaim);
      }
      return jwtBuilder.sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new GeneralException("JWTToken could not be generated");
    }
  }

  @Override
  public User verifyJWTToken(String jwtToken, SecretManager.JWT_CATEGORY category) {
    String jwtPasswordSecret = secretManager.getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      verifier.verify(jwtToken);
      return getUserByEmail(JWT.decode(jwtToken).getClaim("email").asString());
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new GeneralException("JWTToken validation failed");
    } catch (JWTDecodeException | SignatureVerificationException e) {
      throw new InvalidCredentialsException("Invalid JWTToken received, failed to decode the token", USER);
    }
  }

  private boolean isUserAdminOfAnyAccount(User user) {
    return user.getAccounts().stream().anyMatch(account -> {
      List<UserGroup> userGroupList = userGroupService.getUserGroupsByAccountId(account.getUuid(), user);
      if (isNotEmpty(userGroupList)) {
        return userGroupList.stream().anyMatch(userGroup -> {
          AccountPermissions accountPermissions = userGroup.getAccountPermissions();
          if (accountPermissions != null) {
            Set<PermissionType> permissions = accountPermissions.getPermissions();
            if (isNotEmpty(permissions)) {
              return permissions.contains(PermissionType.USER_PERMISSION_MANAGEMENT);
            }
          }
          return false;
        });
      }
      return false;
    });
  }

  @Override
  public boolean isAccountAdmin(String accountId) {
    User user = UserThreadLocal.get();
    if (user == null) {
      return true;
    }
    return isAccountAdmin(user, accountId);
  }

  @Override
  public boolean isAccountAdmin(User user, String accountId) {
    UserRequestContext userRequestContext = user.getUserRequestContext();
    if (userRequestContext == null) {
      return true;
    }

    if (!accountId.equals(userRequestContext.getAccountId())) {
      return false;
    }

    UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();
    return isUserAccountAdmin(userPermissionInfo, accountId);
  }

  @Override
  public boolean isUserAccountAdmin(UserPermissionInfo userPermissionInfo, String accountId) {
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    if (accountPermissionSummary == null) {
      return false;
    }

    Set<PermissionType> permissions = accountPermissionSummary.getPermissions();

    if (isEmpty(permissions)) {
      return false;
    }

    return permissions.contains(PermissionType.ACCOUNT_MANAGEMENT);
  }

  @Override
  public boolean isUserAssignedToAccount(User user, String accountId) {
    return user.getAccounts().stream().anyMatch(account -> account.getUuid().equals(accountId));
  }

  @Override
  public boolean isUserVerified(User user) {
    if (user.getAccounts().size() > 0) {
      AuthenticationMechanism authenticationMechanism = getPrimaryAccount(user).getAuthenticationMechanism();
      return (authenticationMechanism != null && authenticationMechanism.getType().equals("SSO"))
          || user.isEmailVerified();
    }
    logger.warn("User [] has no accounts associated,", user.getEmail());
    return false;
  }

  @Override
  public List<User> getUsersOfAccount(String accountId) {
    Account account = accountService.get(accountId);
    PageRequest<User> pageRequest = aPageRequest().addFilter(UserKeys.accounts, HAS, account).build();
    PageResponse<User> pageResponse = wingsPersistence.query(User.class, pageRequest);
    return pageResponse.getResponse();
  }

  @Override
  public List<User> getUsersWithThisAsPrimaryAccount(String accountId) {
    return getUsersOfAccount(accountId)
        .stream()
        .filter(u -> u.getAccounts().get(0).getUuid().equals(accountId))
        .collect(Collectors.toList());
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism(User user) {
    return getPrimaryAccount(user).getAuthenticationMechanism();
  }

  @Override
  public boolean setDefaultAccount(User user, String accountId) {
    // First we have to make sure the user is assigned to the account
    boolean userAssignedToAccount = isUserAssignedToAccount(user, accountId);
    if (userAssignedToAccount) {
      user.setDefaultAccountId(accountId);
      wingsPersistence.save(user);
      return true;
    } else {
      throw new InvalidRequestException("Can't set default account if the user is not associated with the account");
    }
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<User> users = wingsPersistence.createQuery(User.class).filter(UserKeys.accounts, accountId).asList();
    for (User user : users) {
      delete(accountId, user.getUuid());
    }
  }

  @Override
  public boolean updateLead(String email, String accountId) {
    User user = getUserByEmail(email);
    executorService.submit(() -> {
      try {
        Thread.sleep(30000);
      } catch (InterruptedException e) {
      }
      eventPublishHelper.publishUserRegistrationCompletionEvent(accountId, user);
    });
    return true;
  }

  @Override
  public boolean deleteUsersByEmailAddress(String accountId, List<String> usersToRetain) {
    if (CollectionUtils.isEmpty(usersToRetain)) {
      throw new IllegalArgumentException("'usersToRetain' is empty");
    }

    Set<String> usersToDelete = getUsersOfAccount(accountId)
                                    .stream()
                                    .filter(user -> !usersToRetain.contains(user.getEmail()))
                                    .map(UuidAware::getUuid)
                                    .collect(toSet());

    for (String userToDelete : usersToDelete) {
      delete(accountId, userToDelete);
    }

    return true;
  }

  @Override
  public boolean enableUser(String accountId, String userId, boolean enabled) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new UnauthorizedException(EXC_MSG_USER_DOESNT_EXIST, USER);
    } else if (!isUserAssignedToAccount(user, accountId)) {
      throw new InvalidRequestException("User is not assigned to account", USER);
    }
    if (user.isDisabled() == enabled) {
      user.setDisabled(!enabled);
      wingsPersistence.save(user);
      evictUserFromCache(user.getUuid());
      logger.info("User {} is enabled: {}", user.getEmail(), enabled);
    }
    return true;
  }

  @Override
  public void sendPasswordExpirationWarning(String email, Integer passExpirationDays) {
    User user = getUserByEmail(email);

    if (user == null) {
      throw new InvalidRequestException(
          String.format("Email [%s] exist while sending mail for password expiration.", email));
    }

    String jwtPasswordSecret = getJwtSecret();

    try {
      String token = signupService.createSignupTokeFromSecret(jwtPasswordSecret, email, 1);
      sendPasswordExpirationWarningMail(user, token, passExpirationDays);
    } catch (JWTCreationException | UnsupportedEncodingException exception) {
      throw new GeneralException(EXC_MSG_RESET_PASS_LINK_NOT_GEN);
    }
  }

  @Override
  public String createSignupSecretToken(String email, Integer passExpirationDays) {
    String jwtPasswordSecret = getJwtSecret();
    try {
      return signupService.createSignupTokeFromSecret(jwtPasswordSecret, email, passExpirationDays);
    } catch (JWTCreationException | UnsupportedEncodingException exception) {
      throw new SignupException("Signup secret token can't be generated");
    }
  }

  private void sendPasswordExpirationWarningMail(User user, String token, Integer passExpirationDays) {
    try {
      String resetPasswordUrl = getResetPasswordUrl(token, user);

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("name", user.getName());
      templateModel.put("url", resetPasswordUrl);
      templateModel.put("passExpirationDays", passExpirationDays.toString());

      List<String> toList = new ArrayList();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName("password_expiration_warning")
                                .templateModel(templateModel)
                                .accountId(getPrimaryAccount(user).getUuid())
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Reset password email couldn't be sent", e);
    }
  }

  @Override
  public void sendPasswordExpirationMail(String email) {
    User user = getUserByEmail(email);

    if (user == null) {
      throw new InvalidRequestException(
          String.format("Email [%s] exist while sending mail for password expiration.", email));
    }

    String jwtPasswordSecret = getJwtSecret();

    try {
      String token = signupService.createSignupTokeFromSecret(jwtPasswordSecret, email, 1);
      sendPasswordExpirationMail(user, token);
    } catch (JWTCreationException | UnsupportedEncodingException exception) {
      throw new GeneralException(EXC_MSG_RESET_PASS_LINK_NOT_GEN);
    }
  }

  private void sendPasswordExpirationMail(User user, String token) {
    try {
      String resetPasswordUrl = getResetPasswordUrl(token, user);

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("name", user.getName());
      templateModel.put("url", resetPasswordUrl);

      List<String> toList = new ArrayList();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName("password_expiration")
                                .templateModel(templateModel)
                                .accountId(getPrimaryAccount(user).getUuid())
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Reset password email couldn't be sent", e);
    }
  }

  private String getJwtSecret() {
    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }
    return jwtPasswordSecret;
  }

  @Override
  public boolean passwordExpired(String email) {
    User user = getUserByEmail(email);
    Preconditions.checkNotNull(user, "User is null");
    return user.isPasswordExpired();
  }

  @Override
  public PasswordStrengthViolations checkPasswordViolations(
      String token, PasswordSource passwordSource, String password) {
    Account account = null;
    try {
      if (PasswordSource.PASSWORD_RESET_FLOW == passwordSource) {
        account = getAccountFromResetPasswordToken(token);
      } else if (PasswordSource.SIGN_UP_FLOW == passwordSource) {
        account = getAccountFromInviteId(token);
      } else {
        throw new InvalidRequestException("Incorrect password source provided.", USER);
      }
      return loginSettingsService.getPasswordStrengthCheckViolations(
          account, EncodingUtils.decodeBase64ToString(password).toCharArray());
    } catch (Exception ex) {
      logger.warn("Password violation polling failed for token: [{}]", token, ex);
      throw new InvalidRequestException("Password violation polling failed", USER);
    }
  }

  private Account getAccountFromInviteId(String inviteId) {
    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class).filter("_id", inviteId).get();
    return accountService.get(userInvite.getAccountId());
  }

  private Account getAccountFromResetPasswordToken(String resetPasswordToken) {
    User user = verifyJWTToken(resetPasswordToken, JWT_CATEGORY.PASSWORD_SECRET);
    return accountService.get(user.getDefaultAccountId());
  }

  @Override
  public void sendAccountLockedNotificationMail(User user, int lockoutExpirationTime) {
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("name", user.getName());
    templateModel.put("lockoutExpirationTime", Integer.toString(lockoutExpirationTime));

    List<String> toList = new ArrayList();
    toList.add(user.getEmail());

    EmailData emailData = EmailData.builder()
                              .to(toList)
                              .templateName("user_account_locked_notification")
                              .templateModel(templateModel)
                              .accountId(getPrimaryAccount(user).getUuid())
                              .build();

    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);

    emailNotificationService.send(emailData);
  }

  @Override
  public Account getAccountByIdIfExistsElseGetDefaultAccount(User user, Optional<String> accountId) {
    if (accountId.isPresent()) {
      // First check if the user is associated with the account.
      if (!isUserAssignedToAccount(user, accountId.get())) {
        throw new InvalidRequestException("User is not assigned to account", USER);
      }
      return accountService.get(accountId.get());
    } else {
      Account defaultAccount = accountService.get(user.getDefaultAccountId());
      if (null == defaultAccount) {
        return getPrimaryAccount(user);
      } else {
        return defaultAccount;
      }
    }
  }

  /**
   * User can NOT be disabled/enabled in account status change only when:
   * 1. User belongs to multiple accounts
   * 2. User belongs to Harness user group
   */
  @Override
  public boolean canEnableOrDisable(User user) {
    String email = user.getEmail();
    boolean associatedWithMultipleAccounts = user.getAccounts().size() > 1;
    logger.info("User {} is associated with {} accounts", email, user.getAccounts().size());
    boolean isHarnessUser = harnessUserGroupService.isHarnessSupportUser(user.getUuid());
    logger.info("User {} is in harness user group: {}", email, isHarnessUser);
    boolean result = !(associatedWithMultipleAccounts || isHarnessUser);
    logger.info("User {} can be set to new disabled status: {}", email, result);
    return result;
  }

  @Override
  public String saveUserInvite(UserInvite userInvite) {
    return wingsPersistence.save(userInvite);
  }
}
