package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AccountRole.AccountRoleBuilder.anAccountRole;
import static software.wings.beans.ApplicationRole.ApplicationRoleBuilder.anApplicationRole;
import static software.wings.beans.ErrorCode.DOMAIN_NOT_ALLOWED_TO_REGISTER;
import static software.wings.beans.ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND;
import static software.wings.beans.ErrorCode.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.ROLE_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.UNKNOWN_ERROR;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.USER_INVITATION_DOES_NOT_EXIST;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.WORKFLOW;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.mail.EmailException;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.client.utils.URIBuilder;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.Application;
import software.wings.beans.ApplicationRole;
import software.wings.beans.Base;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.ErrorCode;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;
import software.wings.utils.KryoUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.cache.Cache;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/9/16.
 */
@ValidateOnExecution
@Singleton
public class UserServiceImpl implements UserService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
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
  @Inject private AppService appService;
  @Inject private CacheHelper cacheHelper;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#register(software.wings.beans.User)
   */
  @Override
  public User register(User user) {
    if (!StringUtils.isBlank(user.getEmail())) {
      user.setEmail(user.getEmail().trim());
    }

    if (!StringUtils.isBlank(user.getAccountName())) {
      user.setAccountName(user.getAccountName().trim());
    }

    if (!StringUtils.isBlank(user.getName())) {
      user.setName(user.getName().trim());
    }

    if (!StringUtils.isBlank(user.getCompanyName())) {
      user.setCompanyName(user.getCompanyName().trim());
    }

    if (!domainAllowedToRegister(user.getEmail())) {
      logger.warn("DOMAIN_NOT_ALLOWED_TO_REGISTER for user - {}", user.toString());
      throw new WingsException(DOMAIN_NOT_ALLOWED_TO_REGISTER);
    }
    verifyRegisteredOrAllowed(user.getEmail());
    Account account = setupAccount(user.getAccountName(), user.getCompanyName());
    User savedUser = registerNewUser(user, account);
    executorService.execute(() -> sendVerificationEmail(savedUser));
    return savedUser;
  }

  @Override
  public Account addAccount(Account account, User user) {
    if (!StringUtils.isBlank(account.getAccountName())) {
      account.setAccountName(account.getAccountName().trim());
    }

    if (!StringUtils.isBlank(account.getCompanyName())) {
      account.setCompanyName(account.getCompanyName().trim());
    }

    account = setupAccount(account.getAccountName(), account.getCompanyName());
    addAccountAdminRole(user, account);
    sendSuccessfullyAddedToNewAccountEmail(user, account);
    evictUserFromCache(user.getUuid());
    return account;
  }

  private void sendSuccessfullyAddedToNewAccountEmail(User user, Account account) {
    try {
      String loginUrl = buildAbsoluteUrl(String.format("/login?company=%s&account=%s&email=%s",
          account.getCompanyName(), account.getCompanyName(), user.getEmail()));

      EmailData emailData = EmailData.Builder.anEmailData()
                                .withTo(asList(user.getEmail()))
                                .withRetries(2)
                                .withTemplateName("add_account")
                                .withTemplateModel(ImmutableMap.of(
                                    "name", user.getName(), "url", loginUrl, "company", account.getCompanyName()))
                                .withSystem(true)
                                .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
      logger.error("Add account email couldn't be sent", e);
    }
  }

  private User registerNewUser(User user, Account account) {
    User existingUser = getUserByEmail(user.getEmail());
    if (existingUser == null) {
      user.setAppId(Base.GLOBAL_APP_ID);
      user.getAccounts().add(account);
      user.setEmailVerified(false);
      String hashed = hashpw(new String(user.getPassword()), BCrypt.gensalt());
      user.setPasswordHash(hashed);
      user.setPasswordChangedAt(System.currentTimeMillis());
      user.setRoles(Lists.newArrayList(roleService.getAccountAdminRole(account.getUuid())));
      return save(user);
    } else {
      Map<String, Object> map = new HashMap();
      map.put("name", user.getName());
      map.put("passwordHash", hashpw(new String(user.getPassword()), BCrypt.gensalt()));
      wingsPersistence.updateFields(User.class, existingUser.getUuid(), map);
      return existingUser;
    }
  }

  private User getUserByEmail(String email) {
    return wingsPersistence.createQuery(User.class).field("email").equal(email).get();
  }

  private boolean domainAllowedToRegister(String email) {
    return configuration.getPortal().getAllowedDomainsList().size() == 0
        || configuration.getPortal().getAllowedDomains().contains(email.split("@")[1]);
  }

  private void sendVerificationEmail(User user) {
    EmailVerificationToken emailVerificationToken =
        wingsPersistence.saveAndGet(EmailVerificationToken.class, new EmailVerificationToken(user.getUuid()));
    try {
      String verificationUrl =
          buildAbsoluteUrl(configuration.getPortal().getVerificationUrl() + "/" + emailVerificationToken.getToken());

      EmailData emailData = EmailData.Builder.anEmailData()
                                .withTo(asList(user.getEmail()))
                                .withRetries(2)
                                .withTemplateName("signup")
                                .withTemplateModel(ImmutableMap.of("name", user.getName(), "url", verificationUrl))
                                .withSystem(true)
                                .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
      logger.error("Verification email couldn't be sent", e);
    }
  }

  private String buildAbsoluteUrl(String fragment) throws URISyntaxException {
    String baseURl = configuration.getPortal().getUrl().trim();
    URIBuilder uriBuilder = new URIBuilder(baseURl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public void verifyRegisteredOrAllowed(String email) {
    if (StringUtils.isBlank(email)) {
      throw new WingsException(ErrorCode.INVALID_EMAIL);
    }

    final String emailAddress = email.trim();
    if (!EmailValidator.getInstance().isValid(emailAddress)) {
      throw new WingsException(ErrorCode.INVALID_EMAIL);
    }

    User existingUser = getUserByEmail(emailAddress);

    if (existingUser != null && existingUser.isEmailVerified()) {
      logger.warn("USER_ALREADY_REGISTERED error for existingUser - {}", existingUser.toString());
      throw new WingsException(ErrorCode.USER_ALREADY_REGISTERED);
    }

    if (!domainAllowedToRegister(emailAddress)) {
      logger.warn("USER_DOMAIN_NOT_ALLOWED error for emailAddress - {}", emailAddress);
      throw new WingsException(ErrorCode.USER_DOMAIN_NOT_ALLOWED);
    }
  }

  @Override
  public boolean resendVerificationEmail(String email) {
    User existingUser = getUserByEmail(email);
    if (existingUser == null) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
    }

    sendVerificationEmail(existingUser);
    return true;
  }

  @Override
  public boolean verifyToken(String emailToken) {
    EmailVerificationToken verificationToken = wingsPersistence.createQuery(EmailVerificationToken.class)
                                                   .field("appId")
                                                   .equal(Base.GLOBAL_APP_ID)
                                                   .field("token")
                                                   .equal(emailToken)
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
        .collect(Collectors.toList());
  }

  private UserInvite inviteUser(UserInvite userInvite) {
    Account account = accountService.get(userInvite.getAccountId());
    String inviteId = wingsPersistence.save(userInvite);

    User user = getUserByEmail(userInvite.getEmail());
    if (user == null) {
      user = anUser()
                 .withAccounts(Lists.newArrayList(account))
                 .withEmail(userInvite.getEmail())
                 .withEmailVerified(true)
                 .withName(Constants.NOT_REGISTERED)
                 .withRoles(userInvite.getRoles())
                 .withAppId(Base.GLOBAL_APP_ID)
                 .withEmailVerified(false)
                 .build();
      user = save(user);
      sendNewInvitationMail(userInvite, account);
    } else {
      boolean userAlreadyAddedToAccount =
          user.getAccounts().stream().anyMatch(acc -> acc.getUuid().equals(userInvite.getAccountId()));
      if (userAlreadyAddedToAccount) {
        addRoles(user, userInvite.getRoles());
      } else {
        addAccountRoles(user, account, userInvite.getRoles());
      }
      sendAddedRoleEmail(user, account, userInvite.getRoles());
    }
    return wingsPersistence.get(UserInvite.class, userInvite.getAppId(), inviteId);
  }

  private void sendNewInvitationMail(UserInvite userInvite, Account account) {
    try {
      String inviteUrl = buildAbsoluteUrl(
          String.format("/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s", account.getUuid(),
              account.getAccountName(), account.getCompanyName(), userInvite.getEmail(), userInvite.getUuid()));

      EmailData emailData =
          EmailData.Builder.anEmailData()
              .withTo(asList(userInvite.getEmail()))
              .withRetries(2)
              .withTemplateName("invite")
              .withTemplateModel(ImmutableMap.of("url", inviteUrl, "company", account.getCompanyName()))
              .withSystem(true)
              .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
      logger.error("Invitation email couldn't be sent ", e);
    }
  }

  private void sendAddedRoleEmail(User user, Account account, List<Role> roles) {
    try {
      String loginUrl = buildAbsoluteUrl(String.format("/login?company=%s&account=%s&email=%s",
          account.getCompanyName(), account.getCompanyName(), user.getEmail()));

      EmailData emailData = EmailData.Builder.anEmailData()
                                .withTo(asList(user.getEmail()))
                                .withRetries(2)
                                .withTemplateName("add_role")
                                .withTemplateModel(ImmutableMap.of("name", user.getName(), "url", loginUrl, "company",
                                    account.getCompanyName(), "roles", roles))
                                .withSystem(true)
                                .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
      logger.error("Add account email couldn't be sent", e);
    }
  }

  @Override
  public PageResponse<UserInvite> listInvites(PageRequest<UserInvite> pageRequest) {
    return wingsPersistence.query(UserInvite.class, pageRequest);
  }

  @Override
  public UserInvite getInvite(String accountId, String inviteId) {
    return wingsPersistence.get(UserInvite.class,
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFilter("uuid", Operator.EQ, inviteId).build());
  }

  @Override
  public UserInvite completeInvite(UserInvite userInvite) {
    UserInvite existingInvite = getInvite(userInvite.getAccountId(), userInvite.getUuid());
    if (existingInvite == null) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST);
    }
    if (existingInvite.isCompleted()) {
      return existingInvite;
    }
    if (userInvite.getName() == null || userInvite.getPassword() == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "args", "User name/password");
    }

    Account account = accountService.get(existingInvite.getAccountId());
    User existingUser = getUserByEmail(existingInvite.getEmail());
    if (existingUser == null) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST);
    } else {
      Map<String, Object> map = new HashMap();
      map.put("name", userInvite.getName().trim());
      map.put("passwordHash", hashpw(new String(userInvite.getPassword()), BCrypt.gensalt()));
      map.put("emailVerified", true);
      wingsPersistence.updateFields(User.class, existingUser.getUuid(), map);
    }

    wingsPersistence.updateField(UserInvite.class, existingInvite.getUuid(), "completed", true);
    existingInvite.setCompleted(true);
    return existingInvite;
  }

  @Override
  public UserInvite deleteInvite(String accountId, String inviteId) {
    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class)
                                .field(ID_KEY)
                                .equal(inviteId)
                                .field("accountId")
                                .equal(accountId)
                                .get();
    if (userInvite != null) {
      wingsPersistence.delete(userInvite);
    }
    return userInvite;
  }

  @Override
  public boolean resetPassword(String email) {
    User user = getUserByEmail(email);

    if (user == null) {
      throw new WingsException(INVALID_REQUEST, "message", "Email doesn't exist");
    }

    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    if (jwtPasswordSecret == null) {
      throw new WingsException(INVALID_REQUEST, "message", "incorrect portal setup");
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
      throw new WingsException(UNKNOWN_ERROR, "message", "reset password link could not be generated");
    }
    return true;
  }

  @Override
  public boolean updatePassword(String resetPasswordToken, char[] password) {
    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    if (jwtPasswordSecret == null) {
      throw new WingsException(INVALID_REQUEST, "message", "incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      DecodedJWT jwt = verifier.verify(resetPasswordToken);
      JWT decode = JWT.decode(resetPasswordToken);
      String email = decode.getClaim("email").asString();
      resetUserPassword(email, password, decode.getIssuedAt().getTime());
    } catch (UnsupportedEncodingException exception) {
      throw new WingsException(UNKNOWN_ERROR, "message", "Invalid reset password link");
    } catch (JWTVerificationException exception) {
      throw new WingsException(EXPIRED_TOKEN);
    }
    return true;
  }

  @Override
  public void logout(String userId) {
    authService.invalidateAllTokensForUser(userId);
    evictUserFromCache(userId);
  }

  private void resetUserPassword(String email, char[] password, long tokenIssuedAt) {
    User user = getUserByEmail(email);
    if (user == null) {
      throw new WingsException(INVALID_REQUEST, "message", "Email doesn't exist");
    } else if (user.getPasswordChangedAt() > tokenIssuedAt) {
      throw new WingsException(EXPIRED_TOKEN);
    }

    String hashed = hashpw(new String(password), BCrypt.gensalt());
    wingsPersistence.update(user,
        wingsPersistence.createUpdateOperations(User.class)
            .set("passwordHash", hashed)
            .set("passwordChangedAt", System.currentTimeMillis()));
    executorService.submit(() -> authService.invalidateAllTokensForUser(user.getUuid()));
  }

  private void sendResetPasswordEmail(User user, String token) {
    try {
      String resetPasswordUrl = buildAbsoluteUrl("/reset-password/" + token);

      EmailData emailData = EmailData.Builder.anEmailData()
                                .withTo(asList(user.getEmail()))
                                .withRetries(2)
                                .withTemplateName("reset_password")
                                .withTemplateModel(ImmutableMap.of("name", user.getName(), "url", resetPasswordUrl))
                                .withSystem(true)
                                .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
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
    // TODO: access control has to be done at the upper layer
    //    if (!user.getUuid().equals(UserThreadLocal.get().getUuid())) {
    //      throw new WingsException(INVALID_REQUEST, "message", "Modifying other user's profile not allowed");
    //    }

    Builder<String, Object> builder = ImmutableMap.<String, Object>builder().put("name", user.getName());
    if (user.getPassword() != null && user.getPassword().length > 0) {
      builder.put("passwordHash", hashpw(new String(user.getPassword()), BCrypt.gensalt()));
      builder.put("passwordChangedAt", System.currentTimeMillis());
    }
    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
      builder.put("roles", user.getRoles());
    }
    wingsPersistence.updateFields(User.class, user.getUuid(), builder.build());
    evictUserFromCache(user.getUuid());
    return wingsPersistence.get(User.class, user.getAppId(), user.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<User> list(PageRequest<User> pageRequest) {
    return wingsPersistence.query(User.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#delete(java.lang.String)
   */
  @Override
  public void delete(String userId) {
    if (wingsPersistence.delete(User.class, userId)) {
      evictUserFromCache(userId);
    }
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
    return user;
  }

  @Override
  public User getUserFromCacheOrDB(String userId) {
    Cache<String, User> userCache = cacheHelper.getUserCache();
    User user = userCache.get(userId);

    if (user == null) {
      logger.info("User [{}] not found in Cache. Load it from DB", userId);
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

    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).add("roles", role);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userId);
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
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userId);
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
      throw new WingsException(ErrorCode.ACCOUNT_DOES_NOT_EXIT);
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
      for (ResourceType resourceType :
          Arrays.asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
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
      for (ResourceType resourceType :
          Arrays.asList(APPLICATION, SERVICE, ARTIFACT, DEPLOYMENT, WORKFLOW, ENVIRONMENT)) {
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
    } else {
      // TODO - for Prod support and non prod support
    }

    return null;
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

  private User addAccountAdminRole(User existingUser, Account account) {
    return addAccountRoles(
        existingUser, account, Lists.newArrayList(roleService.getAccountAdminRole(account.getUuid())));
  }

  private User addAccountRoles(User existingUser, Account account, List<Role> roles) {
    UpdateResults updated = wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                                        .field("email")
                                                        .equal(existingUser.getEmail())
                                                        .field("appId")
                                                        .equal(existingUser.getAppId()),
        wingsPersistence.createUpdateOperations(User.class).addToSet("accounts", account).addToSet("roles", roles));
    return existingUser;
  }

  private User addRoles(User user, List<Role> roles) {
    UpdateResults updated = wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                                        .field("email")
                                                        .equal(user.getEmail())
                                                        .field("appId")
                                                        .equal(user.getAppId()),
        wingsPersistence.createUpdateOperations(User.class).addToSet("roles", roles));
    return user;
  }

  private Account setupAccount(String accountName, String companyName) {
    if (isBlank(companyName)) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Company Name Can't be empty");
    }

    if (isBlank(accountName)) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Account Name Can't be empty");
    }

    if (accountService.exists(accountName)) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Account Name should be unique");
    }

    Account account = Account.Builder.anAccount().withAccountName(accountName).withCompanyName(companyName).build();

    return accountService.save(account);
  }
}
