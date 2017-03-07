package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.DOMAIN_NOT_ALLOWED_TO_REGISTER;
import static software.wings.beans.ErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.ROLE_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.USER_ALREADY_REGISTERED;
import static software.wings.beans.ErrorCode.USER_DOES_NOT_EXIST;
import static software.wings.beans.ErrorCode.USER_INVITATION_DOES_NOT_EXIST;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.apache.http.client.utils.URIBuilder;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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
  @Inject private EmailNotificationService<EmailData> emailNotificationService;
  @Inject private MainConfiguration configuration;
  @Inject private RoleService roleService;
  @Inject private AccountService accountService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#register(software.wings.beans.User)
   */
  @Override
  public User register(User user) {
    if (!domainAllowedToRegister(user.getEmail())) {
      throw new WingsException(DOMAIN_NOT_ALLOWED_TO_REGISTER);
    }

    return getUserByEmail(user.getEmail()) != null
        ? addAccountToExistingUser(getUserByEmail(user.getEmail()), user.getCompanyName())
        : registerNewUser(user);
  }

  private List<Role> getAllInvitedRoles(String email, String accountId) {
    UserInvite userInvite = wingsPersistence.createQuery(UserInvite.class)
                                .field("email")
                                .equal(email)
                                .field("accountId")
                                .equal(accountId)
                                .get();
    List<Role> roles = new ArrayList<>();
    if (userInvite != null) {
      roles.addAll(userInvite.getRoles());
    }
    return roles;
  }

  private User addAccountToExistingUser(User existingUser, String companyName) {
    if (isBlank(companyName)) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Company Name Can't be empty");
    } else if (existingUser.getAccounts().stream().anyMatch(account -> account.getCompanyName().equals(companyName))) {
      throw new WingsException(USER_ALREADY_REGISTERED);
    }

    Account account = accountService.findOrCreate(companyName);
    List<Role> invitedRoles = getAllInvitedRoles(existingUser.getEmail(), account.getUuid());
    if (invitedRoles == null || invitedRoles.size() == 0) {
      throw new WingsException(USER_INVITATION_DOES_NOT_EXIST);
    }

    UpdateResults updated = wingsPersistence.update(wingsPersistence.createQuery(User.class)
                                                        .field("email")
                                                        .equal(existingUser.getEmail())
                                                        .field("appId")
                                                        .equal(existingUser.getAppId()),
        wingsPersistence.createUpdateOperations(User.class)
            .addToSet("accounts", account)
            .addToSet("roles", invitedRoles));
    User user = get(existingUser.getUuid());
    markInviteCompleted(existingUser.getEmail(), account.getUuid());
    executorService.execute(() -> sendSuccessfullyAddedToNewAccountEmail(user, account));
    return user;
  }

  private void sendSuccessfullyAddedToNewAccountEmail(User user, Account account) {
    try {
      String loginUrl = buildAbsoluteUrl("/login");

      EmailData emailData = EmailData.Builder.anEmailData()
                                .withTo(asList(user.getEmail()))
                                .withRetries(2)
                                .withTemplateName("add_account")
                                .withTemplateModel(ImmutableMap.of(
                                    "name", user.getName(), "url", loginUrl, "company", account.getCompanyName()))
                                .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
      logger.error("Verification email couldn't be sent " + e);
    }
  }

  private void markInviteCompleted(String email, String accountId) {
    wingsPersistence.update(
        wingsPersistence.createQuery(UserInvite.class).field("email").equal(email).field("accountId").equal(accountId),
        wingsPersistence.createUpdateOperations(UserInvite.class).set("completed", true));
  }

  private User registerNewUser(User user) {
    String companyName =
        isBlank(user.getCompanyName()) ? configuration.getPortal().getCompanyName() : user.getCompanyName();
    Account account = accountService.findOrCreate(companyName);

    user.getAccounts().add(account);

    PageResponse<User> verifiedUsers = list(aPageRequest()
                                                .addFilter("accounts", Operator.EQ, account.getUuid())
                                                .addFilter("emailVerified", Operator.EQ, true)
                                                .build());
    if (verifiedUsers.getResponse().size() == 0) { // first User. Assign admin role
      user.addRole(roleService.getAdminRole());
    } else {
      List<Role> invitedRoles = getAllInvitedRoles(user.getEmail(), account.getUuid());
      if (invitedRoles == null || invitedRoles.size() == 0) {
        throw new WingsException(USER_INVITATION_DOES_NOT_EXIST);
      }
      user.setRoles(invitedRoles);
    }

    user.setEmailVerified(false);
    String hashed = hashpw(user.getPassword(), BCrypt.gensalt());
    user.setPasswordHash(hashed);
    User savedUser = wingsPersistence.saveAndGet(User.class, user);
    markInviteCompleted(savedUser.getEmail(), account.getUuid());
    executorService.execute(() -> sendVerificationEmail(savedUser));
    return savedUser;
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
                                .build();
      emailNotificationService.send(emailData);
    } catch (EmailException | TemplateException | IOException | URISyntaxException e) {
      logger.error("Verification email couldn't be sent " + e);
    }
  }

  private String buildAbsoluteUrl(String fragment) throws URISyntaxException {
    String baseURl = configuration.getPortal().getUrl().trim();
    URIBuilder uriBuilder = new URIBuilder(baseURl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  @Override
  public boolean verifyEmail(String emailToken) {
    EmailVerificationToken verificationToken =
        wingsPersistence.executeGetOneQuery(wingsPersistence.createQuery(EmailVerificationToken.class)
                                                .field("appId")
                                                .equal(Base.GLOBAL_APP_ID)
                                                .field("token")
                                                .equal(emailToken));

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
          userInvite.setEmail(email);
          return inviteUser(userInvite);
        })
        .collect(Collectors.toList());
  }

  private UserInvite inviteUser(UserInvite userInvite) {
    User user = wingsPersistence.createQuery(User.class)
                    .field("email")
                    .equal(userInvite.getEmail())
                    .field("accounts")
                    .equal(userInvite.getAccountId())
                    .get();

    if (user != null) {
      userInvite.getRoles().forEach(role -> addRole(user.getUuid(), role.getUuid()));
      userInvite.setCompleted(true);
    }

    UserInvite existingInvite = wingsPersistence.createQuery(UserInvite.class)
                                    .field("email")
                                    .equal(userInvite.getEmail())
                                    .field("accountId")
                                    .equal(userInvite.getAccountId())
                                    .get();

    if (existingInvite != null) {
      wingsPersistence.addToList(UserInvite.class, existingInvite.getAppId(), existingInvite.getUuid(),
          wingsPersistence.createQuery(UserInvite.class), "roles", userInvite.getRoles());
      return wingsPersistence.get(UserInvite.class, existingInvite.getAppId(), existingInvite.getUuid());
    }

    return wingsPersistence.saveAndGet(UserInvite.class, userInvite);
  }

  @Override
  public PageResponse<UserInvite> listInvites(PageRequest<UserInvite> pageRequest) {
    return wingsPersistence.query(UserInvite.class, pageRequest);
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#matchPassword(java.lang.String, java.lang.String)
   */
  @Override
  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#update(software.wings.beans.User)
   */
  @Override
  public User update(User user) {
    if (!user.getUuid().equals(UserThreadLocal.get().getUuid())) {
      throw new WingsException(INVALID_REQUEST, "message", "Modifying other user's profile not allowed");
    }

    Builder<String, Object> builder = ImmutableMap.<String, Object>builder().put("name", user.getName());
    if (user.getPassword() != null && user.getPassword().length() > 0) {
      builder.put("passwordHash", hashpw(user.getPassword(), BCrypt.gensalt()));
    }
    wingsPersistence.updateFields(User.class, user.getUuid(), builder.build());
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
    wingsPersistence.delete(User.class, userId);
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
    return wingsPersistence.get(User.class, userId);
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
}
