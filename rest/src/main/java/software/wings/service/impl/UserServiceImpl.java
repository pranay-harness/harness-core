package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
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
import static software.wings.beans.ErrorCode.DOMAIN_NOT_ALLOWED_TO_REGISTER;
import static software.wings.beans.ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND;
import static software.wings.beans.ErrorCode.EXPIRED_TOKEN;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.ROLE_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.UNKNOWN_ERROR;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.USER_INVITATION_DOES_NOT_EXIST;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.USER;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.security.PermissionAttribute.ResourceType.ARTIFACT;
import static software.wings.security.PermissionAttribute.ResourceType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.ENVIRONMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.WORKFLOW;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.security.UserGroup;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.SecretManager;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;
import software.wings.utils.KryoUtils;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
  private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
  public static final int JWT_TOKEN_VALIDITY_DURATION = 3 * 60 * 1000; // 3 min

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
    if (isNotBlank(account.getAccountName())) {
      account.setAccountName(account.getAccountName().trim());
    }

    if (isNotBlank(account.getCompanyName())) {
      account.setCompanyName(account.getCompanyName().trim());
    }

    account = setupAccount(account.getAccountName(), account.getCompanyName());
    addAccountAdminRole(user, account);
    authHandler.addUserToDefaultAccountAdminUserGroup(user, account);
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
      user.setAppId(Base.GLOBAL_APP_ID);
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
    if (isBlank(email)) {
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
                                                   .filter("appId", Base.GLOBAL_APP_ID)
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

  private UserInvite inviteUser(UserInvite userInvite) {
    Account account = accountService.get(userInvite.getAccountId());
    String inviteId = wingsPersistence.save(userInvite);

    if (CollectionUtils.isEmpty(userInvite.getRoles())) {
      Role accountAdminRole = roleService.getAccountAdminRole(userInvite.getAccountId());
      if (accountAdminRole != null) {
        List<Role> roleList = new ArrayList<>();
        roleList.add(accountAdminRole);
        userInvite.setRoles(roleList);
      }
    }

    User user = getUserByEmail(userInvite.getEmail());
    if (user == null) {
      user = anUser()
                 .withAccounts(Lists.newArrayList(account))
                 .withEmail(userInvite.getEmail().trim().toLowerCase())
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
      if (StringUtils.equals(user.getName(), Constants.NOT_REGISTERED)) {
        sendNewInvitationMail(userInvite, account);
      } else {
        sendAddedRoleEmail(user, account, userInvite.getRoles());
      }
    }
    return wingsPersistence.get(UserInvite.class, userInvite.getAppId(), inviteId);
  }

  private void sendNewInvitationMail(UserInvite userInvite, Account account) {
    try {
      String inviteUrl =
          buildAbsoluteUrl(format("/invite?accountId=%s&account=%s&company=%s&email=%s&inviteId=%s", account.getUuid(),
              account.getAccountName(), account.getCompanyName(), userInvite.getEmail(), userInvite.getUuid()));

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("url", inviteUrl);
      templateModel.put("company", account.getCompanyName());
      List<String> toList = new ArrayList();
      toList.add(userInvite.getEmail());
      EmailData emailData = EmailData.builder()
                                .to(toList)
                                .templateName(INVITE_EMAIL_TEMPLATE_NAME)
                                .templateModel(templateModel)
                                .accountId(account.getUuid())
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(2);

      emailNotificationService.send(emailData);
    } catch (URISyntaxException e) {
      logger.error("Invitation email couldn't be sent ", e);
    }
  }

  private void sendAddedRoleEmail(User user, Account account, List<Role> roles) {
    try {
      String loginUrl = buildAbsoluteUrl(format("/login?company=%s&account=%s&email=%s", account.getCompanyName(),
          account.getCompanyName(), user.getEmail()));

      Map<String, Object> templateModel = new HashMap<>();
      templateModel.put("name", user.getName());
      templateModel.put("url", loginUrl);
      templateModel.put("company", account.getCompanyName());
      templateModel.put("roles", roles);
      List<String> toList = new ArrayList();
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
    return wingsPersistence.get(UserInvite.class,
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFilter("uuid", Operator.EQ, inviteId).build());
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
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
      throw new InvalidRequestException("User name/password");
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
    UserInvite userInvite =
        wingsPersistence.createQuery(UserInvite.class).filter(ID_KEY, inviteId).filter("accountId", accountId).get();
    if (userInvite != null) {
      wingsPersistence.delete(userInvite);
    }
    return userInvite;
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
      throw new WingsException(UNKNOWN_ERROR).addParam("message", "reset password link could not be generated");
    }
    return true;
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public boolean updatePassword(String resetPasswordToken, char[] password) {
    String jwtPasswordSecret = configuration.getPortal().getJwtPasswordSecret();
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      DecodedJWT jwt = verifier.verify(resetPasswordToken);
      JWT decode = JWT.decode(resetPasswordToken);
      String email = decode.getClaim("email").asString();
      resetUserPassword(email, password, decode.getIssuedAt().getTime());
    } catch (UnsupportedEncodingException exception) {
      throw new WingsException(UNKNOWN_ERROR).addParam("message", "Invalid reset password link");
    } catch (JWTVerificationException exception) {
      throw new WingsException(EXPIRED_TOKEN, USER);
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

    if (user.getPassword() != null && user.getPassword().length > 0) {
      updateOperations.set("passwordHash", hashpw(new String(user.getPassword()), BCrypt.gensalt()));
      updateOperations.set("passwordChangedAt", System.currentTimeMillis());
    }
    if (isNotEmpty(user.getRoles())) {
      updateOperations.set("roles", user.getRoles());
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<User> list(PageRequest<User> pageRequest) {
    PageResponse<User> pageResponse = wingsPersistence.query(User.class, pageRequest);
    if (pageResponse != null) {
      pageResponse.forEach(user -> loadSupportAccounts(user));
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

    PageResponse<UserGroup> pageResponse =
        userGroupService.list(accountId, aPageRequest().addFilter("memberIds", Operator.HAS, user.getUuid()).build());
    List<UserGroup> userGroupList = pageResponse.getResponse();
    userGroupList.stream().forEach(userGroup -> {
      List<User> members = userGroup.getMembers();
      if (isNotEmpty(members)) {
        // Find the user to be removed, then remove from the member list and update the user group.
        Optional<User> userOptional = members.stream().filter(member -> member.getUuid().equals(userId)).findFirst();
        if (userOptional.isPresent()) {
          members.remove(userOptional.get());
          userGroupService.updateMembers(userGroup);
        }
      }
    });

    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class)
                                          .set("roles", user.getRoles())
                                          .set("accounts", user.getAccounts());
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).filter(ID_KEY, userId);
    wingsPersistence.update(updateQuery, updateOp);
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

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
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
      JWSSigner signer = new MACSigner(jwtZendeskSecret.getBytes());
      jwsObject.sign(signer);
    } catch (com.nimbusds.jose.JOSEException e) {
      logger.error("Error signing JWT: " + e.getMessage(), e);
      throw new InvalidRequestException("Error signing JWT: " + e.getMessage());
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

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public User addUserGroups(User user, List<UserGroup> userGroups) {
    UpdateResults updated = wingsPersistence.update(
        wingsPersistence.createQuery(User.class).filter("email", user.getEmail()).filter("appId", user.getAppId()),
        wingsPersistence.createUpdateOperations(User.class).addToSet("userGroups", userGroups));
    return user;
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

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  private User addAccountRoles(User existingUser, Account account, List<Role> roles) {
    UpdateResults updated = wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                                        .filter("email", existingUser.getEmail())
                                                        .filter("appId", existingUser.getAppId()),
        wingsPersistence.createUpdateOperations(User.class).addToSet("accounts", account).addToSet("roles", roles));
    return existingUser;
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  private User addRoles(User user, List<Role> roles) {
    UpdateResults updated = wingsPersistence.update(
        wingsPersistence.createQuery(User.class).filter("email", user.getEmail()).filter("appId", user.getAppId()),
        wingsPersistence.createUpdateOperations(User.class).addToSet("roles", roles));
    return user;
  }

  private Account setupAccount(String accountName, String companyName) {
    if (isBlank(companyName)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Company Name Can't be empty");
    }

    if (isBlank(accountName)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Account Name Can't be empty");
    }

    if (accountService.exists(accountName)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "Account Name should be unique");
    }

    Account account = Account.Builder.anAccount().withAccountName(accountName).withCompanyName(companyName).build();

    return accountService.save(account);
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
      throw new WingsException(UNKNOWN_ERROR, exception).addParam("message", "JWTToken could not be generated");
    }
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  @Override
  public User verifyJWTToken(String jwtToken, SecretManager.JWT_CATEGORY category) {
    String jwtPasswordSecret = secretManager.getJWTSecret(category);
    if (jwtPasswordSecret == null) {
      throw new InvalidRequestException("incorrect portal setup");
    }

    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtPasswordSecret);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
      DecodedJWT jwt = verifier.verify(jwtToken);
      JWT decode = JWT.decode(jwtToken);
      String claimEmail = decode.getClaim("email").asString();
      return getUserByEmail(claimEmail);
    } catch (UnsupportedEncodingException | JWTCreationException exception) {
      throw new WingsException(UNKNOWN_ERROR, exception).addParam("message", "JWTToken validation failed");
    } catch (JWTDecodeException | SignatureVerificationException e) {
      throw new WingsException(INVALID_CREDENTIAL)
          .addParam("message", "Invalid JWTToken received, failed to decode the token");
    }
  }

  @Override
  public boolean isUserAssignedToAccount(User user, String accountId) {
    return user.getAccounts().stream().filter(account -> account.getUuid().equals(accountId)).findFirst().isPresent();
  }
}
