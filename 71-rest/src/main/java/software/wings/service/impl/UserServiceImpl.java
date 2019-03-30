package software.wings.service.impl;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.HAS;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ACCOUNT_DOES_NOT_EXIT;
import static io.harness.eraro.ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_EMAIL;
import static io.harness.eraro.ErrorCode.INVALID_MARKETPLACE_TOKEN;
import static io.harness.eraro.ErrorCode.MARKETPLACE_TOKEN_NOT_FOUND;
import static io.harness.eraro.ErrorCode.ROLE_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_ALREADY_REGISTERED;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_DOMAIN_NOT_ALLOWED;
import static io.harness.eraro.ErrorCode.USER_INVITATION_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
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
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.serializer.KryoUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.client.utils.URIBuilder;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.ApplicationRole;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.EntityType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.MarketPlace;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.UserInviteSource;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.marketplace.MarketPlaceConstants;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.licensing.LicenseService;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.UserThreadLocal;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.security.authentication.oauth.OauthClient;
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
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/9/16.
 */
@ValidateOnExecution
@Singleton
public class UserServiceImpl implements UserService {
  public static final String ADD_GROUP_EMAIL_TEMPLATE_NAME = "add_group";
  public static final String SIGNUP_EMAIL_TEMPLATE_NAME = "signup";
  public static final String INVITE_EMAIL_TEMPLATE_NAME = "invite";
  public static final String TRIAL_EMAIL_VERIFICATION_TEMPLATE_NAME = "invite_trial";
  public static final String TRIAL_SIGNUP_COMPLETED_TEMPLATE_NAME = "trial_signup_completed";
  public static final int REGISTRATION_SPAM_THRESHOLD = 3;

  private static final String BLACKLISTED_DOMAINS_FILE = "trial/blacklisted-email-domains.txt";

  private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

  /**
   * The Executor service.
   */
  @Inject ExecutorService executorService;
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
  @Inject private CacheHelper cacheHelper;
  @Inject private AuthHandler authHandler;
  @Inject private SecretManager secretManager;
  @Inject private TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private SamlClientService samlClientService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private UsageMetricsEventPublisher usageMetricsEventPublisher;
  @Inject private AuthenticationManager authenticationManager;
  @Inject private SSOService ssoService;

  private volatile Set<String> blacklistedDomains = new HashSet<>();

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
    return userInvite;
  }

  /**
   * Trial/Freemium user invitation won't create account. The freemium account will be created only at time of
   * invitation completion.
   */
  @Override
  public boolean trialSignup(String email) {
    final String emailAddress = email.trim().toLowerCase();
    validateTrialSignup(emailAddress);

    UserInvite userInvite = getUserInviteByEmail(emailAddress);
    if (userInvite == null) {
      // Create a new user invite to track the invitation status
      userInvite = new UserInvite();
      userInvite.setSource(UserInviteSource.builder().type(SourceType.TRIAL).build());
      userInvite.setEmail(emailAddress);
      userInvite.setCompleted(false);

      String inviteId = wingsPersistence.save(userInvite);
      userInvite.setUuid(inviteId);

      // Send an email invitation for the trial user to finish up the sign-up with additional information
      // such as password, account/company name information.
      sendVerificationEmail(userInvite);
      eventPublishHelper.publishTrialUserSignupEvent(emailAddress);
    } else if (userInvite.isCompleted()) {
      if (isTrialRegistrationSpam(userInvite)) {
        return false;
      }
      // HAR-7590: If user invite has completed. Send an email saying so and ask the user to login directly.
      sendTrialSignupCompletedEmail(userInvite);
    } else {
      if (isTrialRegistrationSpam(userInvite)) {
        return false;
      }
      // HAR-7250: If the user invite was not completed. Resend the verification/invitation email.
      sendVerificationEmail(userInvite);
    }

    return true;
  }

  private void validateTrialSignup(String email) {
    if (!configuration.isTrialRegistrationAllowed()) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Trial registration is not allowed in this cluster.");
    }

    // Only validate if the email address is valid. Won't check if the email has been registered already.
    checkIfEmailIsValid(email);

    if (containsIllegalCharacters(email)) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "The email used for trial registration contains illegal characters.");
    }

    if (!configuration.isBlacklistedEmailDomainsAllowed()) {
      // lazily populate blacklisted temporary email domains from file.
      populateBlacklistedDomains();

      if (blacklistedDomains.contains(getEmailDomain(email))) {
        throw new WingsException(GENERAL_ERROR, USER)
            .addParam("message", "The domain of the email used for trial registration is not allowed.");
      }
    }
  }

  private String getEmailDomain(String email) {
    return email.substring(email.indexOf('@') + 1);
  }

  private void populateBlacklistedDomains() {
    if (blacklistedDomains.isEmpty()) {
      synchronized (this) {
        if (blacklistedDomains.isEmpty()) {
          try (InputStream inputStream =
                   Thread.currentThread().getContextClassLoader().getResourceAsStream(BLACKLISTED_DOMAINS_FILE);
               BufferedReader bufferedReader =
                   new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = bufferedReader.readLine();
            while (isNotEmpty(line)) {
              blacklistedDomains.add(line.trim().toLowerCase());
              line = bufferedReader.readLine();
            }
            logger.info("Loaded {} temporary email domains into the blacklist.", blacklistedDomains.size());
          } catch (IOException e) {
            logger.error("Failed to read blacklisted temporary email domains from file.", e);
          }
        }
      }
    }
  }

  private boolean containsIllegalCharacters(String email) {
    for (Character illegalChar : ILLEGAL_CHARACTERS) {
      if (email.indexOf(illegalChar) >= 0) {
        return true;
      }
    }
    return false;
  }

  private boolean isTrialRegistrationSpam(UserInvite userInvite) {
    // HAR-7639: If the same email is being used repeatedly for trial signup, it's likely a spam activity.
    // Reject/throttle these registration request to avoid the verification or access-your-account email spamming
    // the legitimate trial user's mailbox.
    Cache<String, Integer> trialEmailCache = cacheHelper.getTrialRegistrationEmailCache();
    String emailAddress = userInvite.getEmail();
    Integer registrationCount = trialEmailCache.get(emailAddress);
    if (registrationCount == null) {
      registrationCount = 1;
    } else {
      registrationCount += 1;
    }
    trialEmailCache.put(emailAddress, registrationCount);
    if (registrationCount > REGISTRATION_SPAM_THRESHOLD) {
      logger.info(
          "Trial registration has been performed already using the email from user invite '{}' shortly before, rejecting this request.",
          userInvite.getUuid());
      return true;
    }
    return false;
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
    return userSummary;
  }

  @Override
  public List<User> getUserSummary(List<User> userList) {
    if (isEmpty(userList)) {
      return Collections.emptyList();
    }
    return userList.stream().map(user -> getUserSummary(user)).collect(toList());
  }

  private void sendSuccessfullyAddedToNewAccountEmail(User user, Account account) {
    try {
      String loginUrl = buildAbsoluteUrl(format("/login?company=%s&account=%s&email=%s", account.getCompanyName(),
          account.getAccountName(), user.getEmail()));

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
      user = wingsPersistence.createQuery(User.class).filter("email", email.trim().toLowerCase()).get();
      loadSupportAccounts(user);
    }

    return user;
  }

  @Override
  public User getUserByEmail(String email, String accountId) {
    User user = null;
    if (isNotEmpty(email)) {
      user = wingsPersistence.createQuery(User.class)
                 .filter("email", email.trim().toLowerCase())
                 .field("accountId")
                 .hasThisOne(accountId)
                 .get();
      loadSupportAccounts(user);
    }

    return user;
  }

  @Override
  public UserInvite getUserInviteByEmail(String email) {
    UserInvite userInvite = null;
    if (isNotEmpty(email)) {
      userInvite = wingsPersistence.createQuery(UserInvite.class).filter("email", email).get();
    }
    return userInvite;
  }

  private UserInvite getUserInviteByEmailAndAccount(String email, String accountId) {
    UserInvite userInvite = null;
    if (isNotEmpty(email)) {
      userInvite =
          wingsPersistence.createQuery(UserInvite.class).filter("email", email).filter("accountId", accountId).get();
    }
    return userInvite;
  }

  private void loadUserGroups(String accountId, User user, boolean loadUsers) {
    List<UserGroup> userGroupList = getUserGroupsOfUser(accountId, user.getUuid(), loadUsers);
    user.setUserGroups(userGroupList);
  }

  private boolean domainAllowedToRegister(String email) {
    return configuration.getPortal().getAllowedDomainsList().isEmpty()
        || configuration.getPortal().getAllowedDomains().contains(email.split("@")[1]);
  }

  private void sendVerificationEmail(User user) {
    EmailVerificationToken emailVerificationToken =
        wingsPersistence.saveAndGet(EmailVerificationToken.class, new EmailVerificationToken(user.getUuid()));
    try {
      String verificationUrl =
          buildAbsoluteUrl(configuration.getPortal().getVerificationUrl() + "/" + emailVerificationToken.getToken());

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

  private String buildAbsoluteUrl(String fragment) throws URISyntaxException {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    String envPath = configuration.getEnvPath();
    if (isNotEmpty(envPath)) {
      String envPathQueryParam = fragment.contains("?") ? "&e=" + envPath.trim() : "?e=" + envPath.trim();
      fragment += envPathQueryParam;
    }

    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public void verifyRegisteredOrAllowed(String email) {
    checkIfEmailIsValid(email);

    final String emailAddress = email.trim();
    if (!domainAllowedToRegister(emailAddress)) {
      throw new WingsException(USER_DOMAIN_NOT_ALLOWED, USER);
    }

    User existingUser = getUserByEmail(emailAddress);
    if (existingUser != null && existingUser.isEmailVerified()) {
      throw new WingsException(USER_ALREADY_REGISTERED, USER);
    }
  }

  private void checkIfEmailIsValid(String email) {
    if (isBlank(email)) {
      throw new WingsException(INVALID_EMAIL, USER).addParam("email", email);
    }

    final String emailAddress = email.trim();
    if (!EmailValidator.getInstance().isValid(emailAddress)) {
      throw new WingsException(INVALID_EMAIL, USER).addParam("email", emailAddress);
    }
  }

  @Override
  public boolean resendVerificationEmail(String email) {
    User existingUser = getUserByEmail(email);
    if (existingUser == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
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
      throw new WingsException(USER_DOES_NOT_EXIST);
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
                                                   .filter("token", emailToken)
                                                   .get();

    if (verificationToken == null) {
      throw new WingsException(EMAIL_VERIFICATION_TOKEN_NOT_FOUND);
    }
    wingsPersistence.updateFields(User.class, verificationToken.getUserId(), ImmutableMap.of("emailVerified", true));
    wingsPersistence.delete(EmailVerificationToken.class, verificationToken.getUuid());
    return true;
  }

  @Override
  public void updateStatsFetchedOnForUser(User user) {
    user.setStatsFetchedOn(System.currentTimeMillis());
    wingsPersistence.updateFields(
        User.class, user.getUuid(), ImmutableMap.of("statsFetchedOn", user.getStatsFetchedOn()));
  }

  @Override
  public List<UserInvite> inviteUsers(UserInvite userInvite) {
    return userInvite.getEmails()
        .stream()
        .map(email -> {
          UserInvite userInviteClone = KryoUtils.clone(userInvite);
          userInviteClone.setEmail(email.trim());
          return inviteUser(userInviteClone);
        })
        .collect(toList());
  }

  @Override
  public UserInvite inviteUser(UserInvite userInvite) {
    // HAR-6861: should validate against invalid email address on user invitation.
    checkIfEmailIsValid(userInvite.getEmail());

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
      boolean emailVerified = !currentAuthenticationMechanism.equals(AuthenticationMechanism.USER_PASSWORD);
      user = anUser()
                 .withAccounts(Lists.newArrayList(account))
                 .withEmail(userInvite.getEmail().trim().toLowerCase())
                 .withName(userInvite.getName().trim())
                 .withRoles(userInvite.getRoles())
                 .withAppId(GLOBAL_APP_ID)
                 .withEmailVerified(emailVerified)
                 .build();
      user = save(user, accountId);
      // Invitation email should sent only in case of USER_PASSWORD authentication mechanism. Because only in that case
      // we need user to set the password.
      if (currentAuthenticationMechanism.equals(AuthenticationMechanism.USER_PASSWORD)) {
        sendNewInvitationMail(userInvite, account);
        sendNotification = false;
      }
    } else {
      boolean userAlreadyAddedToAccount = user.getAccounts().stream().anyMatch(acc -> acc.getUuid().equals(accountId));
      if (userAlreadyAddedToAccount) {
        addRoles(user, userInvite.getRoles());
      } else {
        addAccountRoles(user, account, userInvite.getRoles());
      }
      if (StringUtils.equals(user.getName(), user.getEmail())) {
        sendNewInvitationMail(userInvite, account);
        sendNotification = false;
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
        newUserGroups.add(userGroup.getUuid());
      }
    }

    // Sending email only if user was added to some new group
    if (sendNotification && isNotEmpty(newUserGroups)) {
      sendAddedGroupEmail(user, accountService.get(accountId));
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
                                .filter("email", email)
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

  private String getUserInviteUrl(UserInvite userInvite, Account account) throws URISyntaxException {
    return buildAbsoluteUrl(format("/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s", account.getUuid(),
        account.getAccountName(), account.getCompanyName(), userInvite.getEmail(), userInvite.getUuid()));
  }

  @Override
  public String getUserInviteUrl(String email, Account account) throws URISyntaxException {
    UserInvite userInvite = getUserInviteByEmailAndAccount(email, account.getUuid());
    if (userInvite == null) {
      return null;
    }

    return buildAbsoluteUrl(format("/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s", account.getUuid(),
        account.getAccountName(), account.getCompanyName(), userInvite.getEmail(), userInvite.getUuid()));
  }

  @Override
  public String getUserInviteUrl(String email) throws URISyntaxException {
    UserInvite userInvite = getUserInviteByEmail(email);
    if (userInvite != null) {
      return buildAbsoluteUrl(format("/invite?email=%s&inviteId=%s", userInvite.getEmail(), userInvite.getUuid()));
    }
    return null;
  }

  private Map<String, String> getEmailVerificationTemplateModel(UserInvite userInvite) throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    model.put("name", userInvite.getEmail());
    model.put(
        "url", buildAbsoluteUrl(format("/invite?email=%s&inviteId=%s", userInvite.getEmail(), userInvite.getUuid())));
    return model;
  }

  private Map<String, String> getTrialSignupCompletedTemplatedModel(UserInvite userInvite) throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    String loginUrl = buildAbsoluteUrl("/login");
    model.put("name", userInvite.getEmail());
    model.put("url", loginUrl);
    return model;
  }

  @Override
  public void sendNewInvitationMail(UserInvite userInvite, Account account) {
    try {
      Map<String, String> templateModel = getNewInvitationTemplateModel(userInvite, account);
      sendEmail(userInvite, INVITE_EMAIL_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Invitation email couldn't be sent ", e);
    }
  }

  private void sendVerificationEmail(UserInvite userInvite) {
    try {
      Map<String, String> templateModel = getEmailVerificationTemplateModel(userInvite);
      sendEmail(userInvite, TRIAL_EMAIL_VERIFICATION_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Verification email couldn't be sent ", e);
    }
  }

  public void sendTrialSignupCompletedEmail(UserInvite userInvite) {
    try {
      Map<String, String> templateModel = getTrialSignupCompletedTemplatedModel(userInvite);
      sendEmail(userInvite, TRIAL_SIGNUP_COMPLETED_TEMPLATE_NAME, templateModel);
    } catch (URISyntaxException e) {
      logger.error("Trial sign-up completed email couldn't be sent ", e);
    }
  }

  private void sendEmail(UserInvite userInvite, String templateName, Map<String, String> templateModel) {
    List<String> toList = new ArrayList<>();
    toList.add(userInvite.getEmail());
    EmailData emailData =
        EmailData.builder().to(toList).templateName(templateName).templateModel(templateModel).build();

    emailData.setCc(Collections.emptyList());
    emailData.setRetries(2);
    emailData.setAccountId(userInvite.getAccountId());

    emailNotificationService.send(emailData);
  }

  private Map<String, Object> getAddedRoleTemplateModel(User user, Account account) throws URISyntaxException {
    String loginUrl = buildAbsoluteUrl(format(
        "/login?company=%s&account=%s&email=%s", account.getCompanyName(), account.getAccountName(), user.getEmail()));
    Map<String, Object> model = new HashMap<>();
    model.put("name", user.getName());
    model.put("url", loginUrl);
    model.put("company", account.getCompanyName());
    model.put("email", user.getEmail());
    model.put("authenticationMechanism", account.getAuthenticationMechanism().getType());

    // In case of username-password authentication mechanism, we don't need to add the SSO details in the email.
    if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.USER_PASSWORD)) {
      return model;
    }

    SSOSettings ssoSettings;
    if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.SAML)) {
      ssoSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());
    } else if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.LDAP)) {
      ssoSettings = ssoSettingService.getLdapSettingsByAccountId(account.getUuid());
    } else if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.OAUTH)) {
      ssoSettings = ssoSettingService.getOauthSettingsByAccountId(account.getUuid());
    } else {
      logger.warn("New authentication mechanism detected. Needs to handle the added role email template flow.");
      throw new WingsException("New authentication mechanism detected.");
    }
    model.put("ssoUrl", getDomainName(ssoSettings.getUrl()));
    return model;
  }

  public static String getDomainName(String url) throws URISyntaxException {
    URI uri = new URIBuilder(url).build();
    return uri.getHost();
  }

  @Override
  public void sendAddedGroupEmail(User user, Account account) {
    try {
      Map<String, Object> templateModel = getAddedRoleTemplateModel(user, account);
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

  @Override
  public UserInvite completeInvite(UserInvite userInvite) {
    UserInvite existingInvite = getInvite(userInvite.getAccountId(), userInvite.getUuid());
    if (existingInvite == null) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST, USER);
    }
    if (existingInvite.isCompleted()) {
      return existingInvite;
    }
    if (userInvite.getName() == null || userInvite.getPassword() == null) {
      throw new InvalidRequestException("User name/password is not provided", USER);
    }

    User existingUser = getUserByEmail(existingInvite.getEmail());
    if (existingUser == null) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST, USER);
    } else {
      Map<String, Object> map = new HashMap();
      map.put("name", userInvite.getName().trim());
      map.put("passwordHash", hashpw(new String(userInvite.getPassword()), BCrypt.gensalt()));
      map.put("emailVerified", true);
      wingsPersistence.updateFields(User.class, existingUser.getUuid(), map);
    }

    wingsPersistence.updateField(UserInvite.class, existingInvite.getUuid(), "completed", true);
    wingsPersistence.updateField(UserInvite.class, existingInvite.getUuid(), "agreement", userInvite.isAgreement());
    existingInvite.setCompleted(true);

    eventPublishHelper.publishUserRegistrationCompletionEvent(userInvite.getAccountId(), existingUser);
    return existingInvite;
  }

  @Override
  public User completeInviteAndSignIn(UserInvite userInvite) {
    completeInvite(userInvite);
    return authenticationManager.defaultLogin(userInvite.getEmail(), String.valueOf(userInvite.getPassword()));
  }

  @Override
  public User completeTrialSignupAndSignIn(User user, UserInvite userInvite) {
    completeTrialSignup(user, userInvite);
    return authenticationManager.defaultLogin(userInvite.getEmail(), String.valueOf(userInvite.getPassword()));
  }

  @Override
  public User completeMarketPlaceSignup(User user, UserInvite userInvite) {
    userInvite = marketPlaceSignup(user, userInvite);
    return authenticationManager.defaultLogin(userInvite.getEmail(), String.valueOf(userInvite.getPassword()));
  }

  private UserInvite marketPlaceSignup(User user, UserInvite userInvite) {
    validateUser(user);

    UserInvite existingInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    if (existingInvite == null) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST, USER);
    } else if (existingInvite.isCompleted()) {
      return existingInvite;
    }

    String email = user.getEmail();
    User existingUser = getUserByEmail(email);
    if (existingUser != null) {
      throw new WingsException(USER_ALREADY_REGISTERED, USER);
    }

    if (userInvite.getMarketPlaceToken() == null) {
      throw new WingsException(MARKETPLACE_TOKEN_NOT_FOUND);
    }

    String userInviteID;
    String marketPlaceID;
    try {
      Map<String, Claim> claims =
          secretManager.verifyJWTToken(userInvite.getMarketPlaceToken(), JWT_CATEGORY.MARKETPLACE_SIGNUP);
      userInviteID = claims.get(MarketPlaceConstants.USERINVITE_ID_CLAIM_KEY).asString();
      marketPlaceID = claims.get(MarketPlaceConstants.MARKETPLACE_ID_CLAIM_KEY).asString();
      if (!userInviteID.equals(userInvite.getUuid())) {
        throw new WingsException(String.format(
            "UserInviteID in claim:[{%s}] does not match the userInviteID:[{%s}]", userInviteID, userInvite.getUuid()));
      }
    } catch (Exception e) {
      throw new WingsException(INVALID_MARKETPLACE_TOKEN, e);
    }
    MarketPlace marketPlace = wingsPersistence.get(MarketPlace.class, marketPlaceID);

    if (marketPlace == null) {
      throw new WingsException(String.format("No MarketPlace found with marketPlaceID=[{%s}]", marketPlaceID));
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

    return userInvite;
  }

  private void saveUserAndUserGroups(User user, String email, Account account, List<UserGroup> accountAdminGroups) {
    user.setAppId(GLOBAL_APP_ID);
    user.setEmail(user.getEmail().trim().toLowerCase());
    user.setPasswordHash(hashpw(new String(user.getPassword()), BCrypt.gensalt()));
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
  public UserInvite completeTrialSignup(User user, UserInvite userInvite) {
    if (user.getAccountName() == null || user.getCompanyName() == null) {
      throw new InvalidRequestException("Account/company name is not provided", USER);
    }
    if (isEmpty(user.getName()) || isEmpty(user.getPassword())) {
      throw new InvalidRequestException("User's name/password is not provided", USER);
    }

    UserInvite existingInvite = wingsPersistence.get(UserInvite.class, userInvite.getUuid());
    if (existingInvite == null) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST, USER);
    } else if (existingInvite.isCompleted()) {
      return existingInvite;
    }

    String email = existingInvite.getEmail();
    User existingUser = getUserByEmail(email);
    if (existingUser != null) {
      throw new WingsException(USER_ALREADY_REGISTERED, USER);
    }

    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
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
    return userInvite;
  }

  private void completeUserInviteForSignup(UserInvite userInvite, String accountId) {
    userInvite.setAccountId(accountId);
    userInvite.setCompleted(true);
    userInvite.setAgreement(true);

    // Update existing invite with the associated account ID and set the status to be completed.
    wingsPersistence.save(userInvite);
  }

  @Override
  public User signUpUserUsingOauth(OauthUserInfo userInfo, OauthClient oauthClient) {
    logger.info(String.format("User not found in db. Creating an account for: [%s]", userInfo.getEmail()));
    checkForFreemiumCluster();
    final User user = createUser(userInfo, oauthClient.getName());
    notNullOrEmptyCheck(user.getAccountName(), "Account/Company name");
    notNullOrEmptyCheck(user.getName(), "User's name");

    throwExceptionIfUserIsAlreadyRegistered(user.getEmail());

    // Create a trial account whose license expires in 15 days.
    Account account = createAccountWithTrialLicense(user);

    // For trial user just signed up, it will be assigned to the account admin role.
    assignUserToAccountAdminGroup(user, account);

    String accountId = account.getUuid();
    createSSOSettingsAndMarkAsDefaultAuthMechanism(accountId, oauthClient);

    eventPublishHelper.publishUserRegistrationCompletionEvent(accountId, user);
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
        .withEmail(email)
        .withName(userInfo.getName())
        .withAccountName(companyName)
        .withCompanyName(companyName)
        .withEmailVerified(true)
        .withOauthProvider(oauthProvider)
        .build();
  }

  private void notNullOrEmptyCheck(String name, String errorSubject) {
    if (Strings.isNullOrEmpty(name)) {
      throw new InvalidRequestException(errorSubject + " is empty", USER);
    }
  }

  private void assignUserToAccountAdminGroup(User user, Account account) {
    List<UserGroup> accountAdminGroups = getAccountAdminGroup(account.getUuid());

    user.setAppId(GLOBAL_APP_ID);
    user.getAccounts().add(account);
    user.setUserGroups(accountAdminGroups);
    save(user, account.getUuid());

    addUserToUserGroups(account.getUuid(), user, accountAdminGroups, false);
  }

  private void throwExceptionIfUserIsAlreadyRegistered(final String email) {
    User existingUser = getUserByEmail(email);
    if (existingUser != null) {
      throw new WingsException(USER_ALREADY_REGISTERED, USER);
    }
  }

  private Account createAccountWithTrialLicense(User user) {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
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

  private void createSSOSettingsAndMarkAsDefaultAuthMechanism(String accountId, OauthClient oauthClient) {
    OauthSettings oauthSettings = OauthSettings.builder()
                                      .accountId(accountId)
                                      .displayName(oauthClient.getName())
                                      .url(oauthClient.getRedirectUrl().toString())
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
                                 .filter("email", email);
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
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "reset password link could not be generated");
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
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Invalid reset password link");
    } catch (JWTVerificationException exception) {
      throw new WingsException(EXPIRED_TOKEN, USER);
    }
    return true;
  }

  @Override
  public void logout(User user) {
    authService.invalidateToken(user.getToken());
    evictUserFromCache(user.getUuid());
    Account account = user.getAccounts().get(0);
    usageMetricsEventPublisher.publishUserLogoutEvent(account.getUuid(), account.getAccountName());
  }

  private void resetUserPassword(String email, char[] password, long tokenIssuedAt) {
    User user = getUserByEmail(email);
    if (user == null) {
      throw new InvalidRequestException("Email doesn't exist");
    } else if (user.getPasswordChangedAt() > tokenIssuedAt) {
      throw new WingsException(EXPIRED_TOKEN, USER);
    }

    String hashed = hashpw(new String(password), BCrypt.gensalt());
    wingsPersistence.update(user,
        wingsPersistence.createUpdateOperations(User.class)
            .set("passwordHash", hashed)
            .set("passwordChangedAt", System.currentTimeMillis()));
    executorService.submit(() -> authService.invalidateAllTokensForUser(user.getUuid()));
  }

  @Override
  public boolean isTwoFactorEnabledForAdmin(String accountId, String userId) {
    // Check if admin has 2FA enabled
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
    }
    return user.isTwoFactorAuthenticationEnabled();
  }

  @Override
  public boolean overrideTwoFactorforAccount(String accountId, User user, boolean adminOverrideTwoFactorEnabled) {
    try {
      Query<User> updateQuery = wingsPersistence.createQuery(User.class);
      updateQuery.filter("accounts", accountId);
      if (updateQuery.count() > 0) {
        for (User u : updateQuery) {
          // Look for user who has only 1 account
          if (u.getAccounts().size() == 1) {
            if (!u.isTwoFactorAuthenticationEnabled()) {
              u.setTwoFactorAuthenticationEnabled(true);
              u.setTwoFactorAuthenticationMechanism(TwoFactorAuthenticationMechanism.TOTP);
              update(u);
              twoFactorAuthenticationManager.sendTwoFactorAuthenticationResetEmail(u.getUuid());
            }
          }
        }
      }
    } catch (Exception ex) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Exception occurred while enforcing Two factor authentication for users");
    }

    return true;
  }

  private void sendResetPasswordEmail(User user, String token) {
    try {
      String resetPasswordUrl = buildAbsoluteUrl("/reset-password/" + token);

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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#matchPassword(java.lang.String, java.lang.String)
   */
  @Override
  public boolean matchPassword(char[] password, String hash) {
    return BCrypt.checkpw(new String(password), hash);
  }

  private User save(User user, String accountId) {
    user = wingsPersistence.saveAndGet(User.class, user);
    evictUserFromCache(user.getUuid());
    eventPublishHelper.publishSetupRbacEvent(accountId, user.getUuid(), EntityType.USER);
    return user;
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
    if (user.getTwoFactorAuthenticationMechanism() != null) {
      updateOperations.set("twoFactorAuthenticationMechanism", user.getTwoFactorAuthenticationMechanism());
    } else {
      updateOperations.unset("twoFactorAuthenticationMechanism");
    }
    if (user.getTotpSecretKey() != null) {
      updateOperations.set("totpSecretKey", user.getTotpSecretKey());
    } else {
      updateOperations.unset("totpSecretKey");
    }

    if (user.getMarketoLeadId() > 0) {
      updateOperations.set("marketoLeadId", user.getMarketoLeadId());
    }

    if (isNotEmpty(user.getReportedMarketoCampaigns())) {
      updateOperations.set("reportedMarketoCampaigns", user.getReportedMarketoCampaigns());
    }

    if (user.getLastLogin() > 0L) {
      updateOperations.set("lastLogin", user.getLastLogin());
    }

    wingsPersistence.update(user, updateOperations);
    evictUserFromCache(user.getUuid());
    return wingsPersistence.getWithAppId(User.class, user.getAppId(), user.getUuid());
  }

  @Override
  public User updateName(String userId, String name) {
    ensureUserExists(userId);
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).filter(ID_KEY, userId);
    updateOperations.set("name", name);
    wingsPersistence.update(updateQuery, updateOperations);
    evictUserFromCache(userId);
    return wingsPersistence.get(User.class, userId);
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

  @Override
  public User updateUserGroupsAndNameOfUser(
      String userId, List<UserGroup> userGroups, String name, String accountId, boolean sendNotification) {
    updateName(userId, name);
    return updateUserGroupsOfUser(userId, userGroups, accountId, sendNotification);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<User> list(PageRequest<User> pageRequest, boolean loadUserGroups) {
    String accountId = null;
    SearchFilter searchFilter = pageRequest.getFilters()
                                    .stream()
                                    .filter(filter -> filter.getFieldName().equals("accounts"))
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
                                            .set("accounts", user.getAccounts());
      Query<User> updateQuery = wingsPersistence.createQuery(User.class).filter(ID_KEY, userId);
      wingsPersistence.update(updateQuery, updateOp);

      removeUserFromUserGroups(user, user.getUserGroups(), false);

      evictUserFromCache(userId);
    });
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#get(java.lang.String)
   */
  @Override
  public User get(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
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
      throw new WingsException(USER_DOES_NOT_EXIST);
    }

    loadSupportAccounts(user);
    loadUserGroups(accountId, user, false);
    return user;
  }

  @Override
  public User getUserFromCacheOrDB(String userId) {
    Cache<String, User> userCache = cacheHelper.getUserCache();
    User user;
    try {
      user = userCache.get(userId);

      if (user == null) {
        logger.info("User [{}] not found in Cache. Load it from DB", userId);
        user = get(userId);
        userCache.put(user.getUuid(), user);
      }
      return user;
    } catch (Exception ex) {
      // If there was any exception, remove that entry from cache
      userCache.remove(userId);
      user = get(userId);
      userCache.put(user.getUuid(), user);
    }

    return user;
  }

  @Override
  public void evictUserFromCache(String userId) {
    cacheHelper.getUserCache().remove(userId);
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
      throw new WingsException(ACCOUNT_DOES_NOT_EXIT);
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
      throw new WingsException(ROLE_DOES_NOT_EXIST);
    }
    return role;
  }

  private void ensureUserExists(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST);
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
      updateOperations.addToSet("accounts", account);
    }
    wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                .filter("email", existingUser.getEmail())
                                .filter("appId", existingUser.getAppId()),
        updateOperations);
  }

  private void addRoles(User user, List<Role> roles) {
    if (isNotEmpty(roles)) {
      UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(User.class);
      updateOperations.addToSet("roles", roles);
      wingsPersistence.update(
          wingsPersistence.createQuery(User.class).filter("email", user.getEmail()).filter("appId", user.getAppId()),
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
    return accountService.save(account);
  }

  private List<UserGroup> getAccountAdminGroup(String accountId) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter(UserGroup.ACCOUNT_ID_KEY, EQ, accountId)
                                             .addFilter("name", EQ, Constants.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME)
                                             .build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    return pageResponse.getResponse();
  }

  @Override
  public String generateJWTToken(String userId, SecretManager.JWT_CATEGORY category) {
    String jwtPasswordSecret = secretManager.getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      return JWT.create()
          .withIssuer("Harness Inc")
          .withIssuedAt(new Date())
          .withExpiresAt(new Date(System.currentTimeMillis() + category.getValidityDuration()))
          .withClaim("email", userId)
          .sign(algorithm);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(GENERAL_ERROR, exception).addParam("message", "JWTToken could not be generated");
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
      throw new WingsException(GENERAL_ERROR, exception).addParam("message", "JWTToken validation failed");
    } catch (JWTDecodeException | SignatureVerificationException e) {
      throw new WingsException(INVALID_CREDENTIAL, USER)
          .addParam("message", "Invalid JWTToken received, failed to decode the token");
    }
  }

  @Override
  public boolean isUserAssignedToAccount(User user, String accountId) {
    return user.getAccounts().stream().anyMatch(account -> account.getUuid().equals(accountId));
  }

  @Override
  public List<String> fetchUserEmailAddressesFromUserIds(List<String> userIds) {
    if (isEmpty(userIds)) {
      return asList();
    }

    return wingsPersistence.createQuery(User.class, excludeAuthority)
        .field(User.ID_KEY)
        .in(userIds)
        .project(User.EMAIL_KEY, true)
        .project(User.EMAIL_VERIFIED_KEY, true)
        .asList()
        .stream()
        .filter(User::isEmailVerified)
        .map(user -> user.getEmail())
        .collect(toList());
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
    PageRequest<User> pageRequest = aPageRequest().addFilter("accounts", HAS, account).build();
    PageResponse<User> pageResponse = wingsPersistence.query(User.class, pageRequest);
    return pageResponse.getResponse();
  }

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
    List<User> users = wingsPersistence.createQuery(User.class).filter(User.ACCOUNT_ID_KEY, accountId).asList();
    for (User user : users) {
      wingsPersistence.delete(User.class, user.getUuid());
      evictUserFromCache(user.getUuid());
    }
  }
}
