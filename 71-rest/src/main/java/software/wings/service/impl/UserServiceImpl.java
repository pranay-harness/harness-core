package software.wings.service.impl;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ACCOUNT_DOES_NOT_EXIT;
import static io.harness.eraro.ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.INVALID_EMAIL;
import static io.harness.eraro.ErrorCode.ROLE_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_ALREADY_REGISTERED;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_DOMAIN_NOT_ALLOWED;
import static io.harness.eraro.ErrorCode.USER_INVITATION_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AccountRole.AccountRoleBuilder.anAccountRole;
import static software.wings.beans.ApplicationRole.ApplicationRoleBuilder.anApplicationRole;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.WORKFLOW;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
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
import software.wings.beans.LicenseInfo;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.UserInviteSource;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.SSOSettings;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.SecretManager;
import software.wings.security.UserThreadLocal;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.security.saml.SamlClientService;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;
import software.wings.utils.KryoUtils;
import software.wings.utils.Misc;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
  public static final String ADD_ROLE_EMAIL_TEMPLATE_NAME = "add_role";
  public static final String SIGNUP_EMAIL_TEMPLATE_NAME = "signup";
  public static final String INVITE_EMAIL_TEMPLATE_NAME = "invite";
  public static final String INVITE_TRIAL_EMAIL_TEMPLATE_NAME = "invite_trial";
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

    Account account = setupAccount(user.getAccountName(), user.getCompanyName());
    User savedUser = registerNewUser(user, account);
    executorService.execute(() -> sendVerificationEmail(savedUser));
    return savedUser;
  }

  /**
   * Trial/Freemium user invitation won't create account. The freemium account will be created only at time of
   * invitation completion.
   */
  @Override
  public boolean trialSignup(String email) {
    if (!configuration.isTrialRegistrationAllowed()) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "Trial user/account registration is disabled.");
    }

    final String emailAddress = email.trim().toLowerCase();
    verifyEmailRegisteredOrAllowed(emailAddress, false);

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
      sendNewInvitationMail(userInvite, null);
    } else if (userInvite.isCompleted()) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "User invite for " + email + " has been completed.");
    } else {
      throw new WingsException(GENERAL_ERROR)
          .addParam("message",
              "User invite for " + email + " exists and is pending, "
                  + "please finish the signup process through your verification email.");
    }

    return true;
  }

  @Override
  public Account addAccount(Account account, User user) {
    if (isNotBlank(account.getAccountName())) {
      account.setAccountName(account.getAccountName().trim());
    }

    if (isNotBlank(account.getCompanyName())) {
      account.setCompanyName(account.getCompanyName().trim());
    }

    account = setupAccount(account);
    addAccountAdminRole(user, account);
    authHandler.addUserToDefaultAccountAdminUserGroup(user, account, true);
    sendSuccessfullyAddedToNewAccountEmail(user, account);
    evictUserFromCache(user.getUuid());
    return account;
  }

  private void sendSuccessfullyAddedToNewAccountEmail(User user, Account account) {
    try {
      String loginUrl = buildAbsoluteUrl(format("/login?company=%s&account=%s&email=%s", account.getCompanyName(),
          account.getCompanyName(), user.getEmail()));

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
                                .system(true)
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
    User existingUser = getUserByEmail(user.getEmail());
    if (existingUser == null) {
      user.setAppId(GLOBAL_APP_ID);
      user.getAccounts().add(account);
      user.setEmailVerified(false);
      String hashed = hashpw(new String(user.getPassword()), BCrypt.gensalt());
      user.setPasswordHash(hashed);
      user.setPasswordChangedAt(System.currentTimeMillis());
      user.setRoles(Lists.newArrayList(roleService.getAccountAdminRole(account.getUuid())));
      return save(user);
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

  private UserInvite getUserInviteByEmail(String email) {
    UserInvite userInvite = null;
    if (isNotEmpty(email)) {
      userInvite = wingsPersistence.createQuery(UserInvite.class).filter("email", email).get();
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
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public void verifyRegisteredOrAllowed(String email) {
    verifyEmailRegisteredOrAllowed(email, true);
  }

  private void verifyEmailRegisteredOrAllowed(String email, boolean verifyAllowedDomain) {
    if (isBlank(email)) {
      throw new WingsException(INVALID_EMAIL, USER);
    }

    final String emailAddress = email.trim();
    if (!EmailValidator.getInstance().isValid(emailAddress)) {
      throw new WingsException(INVALID_EMAIL, USER);
    }

    if (verifyAllowedDomain && !domainAllowedToRegister(emailAddress)) {
      throw new WingsException(USER_DOMAIN_NOT_ALLOWED, USER);
    }

    User existingUser = getUserByEmail(emailAddress);
    if (existingUser != null && existingUser.isEmailVerified()) {
      throw new WingsException(USER_ALREADY_REGISTERED, USER);
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
      user = save(user);
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
      PageRequest<UserGroup> pageRequest =
          aPageRequest().addFilter("accountId", EQ, accountId).addFilter("_id", IN, userGroupIds.toArray()).build();
      PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
      userGroups = pageResponse.getResponse();
      addUserToUserGroups(accountId, user, userGroups, sendNotification);
    }
    return wingsPersistence.get(UserInvite.class, userInvite.getAppId(), inviteId);
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
      sendAddedRoleEmail(user, accountService.get(accountId));
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

  private List<UserGroup> getUserGroupsOfUser(String accountId, String userId, boolean loadUsers) {
    PageRequest<UserGroup> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("memberIds", EQ, userId).build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, loadUsers);
    return pageResponse.getResponse();
  }

  private List<UserGroup> getUserGroups(String accountId, SetView<String> userGroupIds) {
    PageRequest<UserGroup> pageRequest =
        aPageRequest().addFilter("_id", IN, userGroupIds.toArray()).addFilter("accountId", EQ, accountId).build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    return pageResponse.getResponse();
  }

  private Map<String, String> getNewInvitationTemplateModel(UserInvite userInvite, Account account)
      throws URISyntaxException {
    Map<String, String> model = new HashMap<>();
    final String inviteUrl;
    if (account == null) {
      inviteUrl = buildAbsoluteUrl(format("/invite?email=%s&inviteId=%s", userInvite.getEmail(), userInvite.getUuid()));
    } else {
      inviteUrl =
          buildAbsoluteUrl(format("/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s", account.getUuid(),
              account.getAccountName(), account.getCompanyName(), userInvite.getEmail(), userInvite.getUuid()));
      model.put("company", account.getCompanyName());
    }
    model.put("name", userInvite.getEmail());
    model.put("url", inviteUrl);
    return model;
  }

  @Override
  public void sendNewInvitationMail(UserInvite userInvite, Account account) {
    try {
      Map<String, String> templateModel = getNewInvitationTemplateModel(userInvite, account);
      List<String> toList = new ArrayList<>();
      toList.add(userInvite.getEmail());
      final EmailData emailData;
      if (account == null) {
        emailData = EmailData.builder()
                        .to(toList)
                        .templateName(INVITE_TRIAL_EMAIL_TEMPLATE_NAME)
                        .templateModel(templateModel)
                        .build();
      } else {
        emailData = EmailData.builder()
                        .to(toList)
                        .templateName(INVITE_EMAIL_TEMPLATE_NAME)
                        .templateModel(templateModel)
                        .accountId(account.getUuid())
                        .build();
      }

      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Invitation email couldn't be sent ", e);
    }
  }

  private Map<String, Object> getAddedRoleTemplateModel(User user, Account account) throws URISyntaxException {
    String loginUrl = buildAbsoluteUrl(format(
        "/login?company=%s&account=%s&email=%s", account.getCompanyName(), account.getAccountName(), user.getEmail()));
    boolean includeAccessUrl = true;
    Map<String, Object> model = new HashMap<>();
    model.put("name", user.getName());
    model.put("url", loginUrl);
    model.put("company", account.getCompanyName());
    model.put("email", user.getEmail());
    model.put("authenticationMechanism", account.getAuthenticationMechanism().getType());
    model.put("includeAccessUrl", true);

    // In case of username-password authentication mechanism, we don't need to add the SSO details in the email.
    if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.USER_PASSWORD)) {
      return model;
    }

    SSOSettings ssoSettings;
    if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.SAML)) {
      ssoSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());
      switch (samlClientService.getHostType(ssoSettings.getUrl())) {
        case GOOGLE:
          includeAccessUrl = false;
          break;
        case AZURE:
          includeAccessUrl = false;
          break;
        default:
      }
    } else if (account.getAuthenticationMechanism().equals(AuthenticationMechanism.LDAP)) {
      ssoSettings = ssoSettingService.getLdapSettingsByAccountId(account.getUuid());
    } else {
      logger.warn("New authentication mechanism detected. Needs to handle the added role email template flow.");
      throw new WingsException("New authentication mechanism detected.");
    }
    model.put("includeAccessUrl", includeAccessUrl);
    model.put("ssoUrl", ssoSettings.getUrl());
    return model;
  }

  @Override
  public void sendAddedRoleEmail(User user, Account account) {
    try {
      Map<String, Object> templateModel = getAddedRoleTemplateModel(user, account);
      List<String> toList = new ArrayList<>();
      toList.add(user.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName(ADD_ROLE_EMAIL_TEMPLATE_NAME)
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
    return wingsPersistence.get(
        UserInvite.class, aPageRequest().addFilter("accountId", EQ, accountId).addFilter("uuid", EQ, inviteId).build());
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
    return existingInvite;
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

    userInvite.setAccountId(accountId);
    userInvite.setCompleted(true);
    userInvite.setAgreement(true);

    // Update existing invite with the associated account ID and set the status to be completed.
    wingsPersistence.save(userInvite);

    user.setAppId(GLOBAL_APP_ID);
    user.setEmail(email);
    user.setPasswordHash(hashpw(new String(user.getPassword()), BCrypt.gensalt()));
    user.setEmailVerified(true);
    user.getAccounts().add(account);
    user.setUserGroups(accountAdminGroups);

    save(user);

    addUserToUserGroups(accountId, user, accountAdminGroups, false);

    return userInvite;
  }

  @Override
  public UserInvite deleteInvite(String accountId, String inviteId) {
    UserInvite userInvite =
        wingsPersistence.createQuery(UserInvite.class).filter(ID_KEY, inviteId).filter("accountId", accountId).get();
    if (userInvite != null) {
      wingsPersistence.delete(userInvite);
    }
    return userInvite;
  }

  @Override
  public boolean deleteInvites(String accountId, String email) {
    Query userInvitesQuery =
        wingsPersistence.createQuery(UserInvite.class).filter("accountId", accountId).filter("email", email);
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
      if (updateQuery != null && updateQuery.count() > 0) {
        for (User u : updateQuery) {
          // Look for user who has only 1 account
          if (u.getAccounts() != null && u.getAccounts().size() == 1
              && u.getAccounts().get(0).getUuid().equals(accountId)) {
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

  private User save(User user) {
    user = wingsPersistence.saveAndGet(User.class, user);
    evictUserFromCache(user.getUuid());
    return user;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#update(software.wings.beans.User)
   */
  @Override
  public User update(User user) {
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);

    updateOperations.set("emailVerified", user.isEmailVerified());
    if (isNotEmpty(user.getAccounts())) {
      updateOperations.set("accounts", user.getAccounts());
    }

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

    wingsPersistence.update(user, updateOperations);
    evictUserFromCache(user.getUuid());
    return wingsPersistence.get(User.class, user.getAppId(), user.getUuid());
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

    authService.evictAccountUserPermissionInfoCache(accountId, Arrays.asList(userId));
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
  public PageResponse<User> list(PageRequest<User> pageRequest) {
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

    final String accountIdFinal = accountId;
    PageResponse<User> pageResponse = wingsPersistence.query(User.class, pageRequest);
    if (pageResponse != null) {
      pageResponse.forEach(user -> {
        loadSupportAccounts(user);
        if (accountIdFinal != null) {
          loadUserGroups(accountIdFinal, user, false);
        }
      });
    }
    return pageResponse;
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

    PageResponse<UserGroup> pageResponse = userGroupService.list(
        accountId, aPageRequest().addFilter("memberIds", Operator.HAS, user.getUuid()).build(), true);
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
      accounts.forEach(account -> accountService.decryptLicenseInfo(account, false));
    }

    return user;
  }

  private void loadSupportAccounts(User user) {
    if (user == null) {
      return;
    }

    Set<String> excludeAccounts = user.getAccounts().stream().map(Account::getUuid).collect(Collectors.toSet());
    List<Account> accountList =
        harnessUserGroupService.listAllowedSupportAccountsForUser(user.getUuid(), excludeAccounts);
    user.setSupportAccounts(accountList);
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
      logger.error("Error signing JWT: " + Misc.getMessage(e), e);
      throw new InvalidRequestException("Error signing JWT: " + Misc.getMessage(e));
    }

    // Serialise to JWT compact form
    String jwtString = jwsObject.serialize();

    String redirectUrl = "https://"
        + "harnesssupport.zendesk.com/access/jwt?jwt=" + jwtString;

    if (returnToUrl != null) {
      try {
        redirectUrl += "&return_to=" + encode(redirectUrl, Charset.defaultCharset().name());
      } catch (UnsupportedEncodingException e) {
        throw new WingsException(e);
      }
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

  private Account setupAccount(String accountName, String companyName) {
    Account account = Account.Builder.anAccount().withAccountName(accountName).withCompanyName(companyName).build();
    return setupAccount(account);
  }

  private Account setupAccount(Account account) {
    if (isBlank(account.getCompanyName())) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "Company Name can't be empty");
    }

    if (isBlank(account.getAccountName())) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "Account Name can't be empty");
    }

    if (accountService.exists(account.getAccountName())) {
      throw new WingsException(GENERAL_ERROR)
          .addParam("message", "Account Name is already taken, please try a different one.");
    }

    account.setAppId(GLOBAL_APP_ID);
    return accountService.save(account);
  }

  private List<UserGroup> getAccountAdminGroup(String accountId) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("accountId", EQ, accountId)
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
}
