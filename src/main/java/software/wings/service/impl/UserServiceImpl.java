package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.DOMAIN_NOT_ALLOWED_TO_REGISTER;
import static software.wings.beans.ErrorCodes.EMAIL_VERIFICATION_TOKEN_NOT_FOUND;
import static software.wings.beans.ErrorCodes.USER_ALREADY_REGISTERED;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/9/16.
 */
@ValidateOnExecution
public class UserServiceImpl implements UserService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private NotificationService<EmailData> emailNotificationService;
  @Inject private MainConfiguration configuration;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#register(software.wings.beans.User)
   */
  public User register(User user) {
    if (!domainAllowedToRegister(user.getEmail())) {
      throw new WingsException(DOMAIN_NOT_ALLOWED_TO_REGISTER);
    }

    if (userAlreadyRegistered(user)) {
      throw new WingsException(USER_ALREADY_REGISTERED);
    }
    user.setEmailVerified(false);
    String hashed = hashpw(user.getPassword(), BCrypt.gensalt());
    user.setPasswordHash(hashed);
    User savedUser = wingsPersistence.saveAndGet(User.class, user);
    sendVerificationEmail(savedUser);
    return savedUser;
  }

  private boolean domainAllowedToRegister(String email) {
    return configuration.getPortal().getAllowedDomains().size() == 0
        || configuration.getPortal().getAllowedDomains().contains(email.split("@")[1]);
  }

  private void sendVerificationEmail(User user) {
    EmailVerificationToken emailVerificationToken =
        wingsPersistence.saveAndGet(EmailVerificationToken.class, new EmailVerificationToken(user.getUuid()));
    try {
      String baseURl = configuration.getPortal().getUrl();
      if (baseURl.charAt(baseURl.length() - 1) != '/') {
        baseURl += "/";
      }
      String relativeApiPath = "api/users/verify/" + emailVerificationToken.getToken();
      String apiUrl = baseURl + relativeApiPath;

      emailNotificationService.send(
          asList(user.getEmail()), asList(), "Please verify your Wings Platform email address", apiUrl);
    } catch (EmailException | TemplateException | IOException e) {
      logger.error("Verification email couldn't be sent " + e);
    }
  }

  public User verifyEmail(String emailToken) {
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
    return get(verificationToken.getUuid());
  }

  private boolean userAlreadyRegistered(User user) {
    return wingsPersistence.createQuery(User.class)
               .field("appId")
               .equal(user.getAppId())
               .field("email")
               .equal(user.getEmail())
               .get()
        != null;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#matchPassword(java.lang.String, java.lang.String)
   */
  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#addRole(java.lang.String, java.lang.String)
   */
  public User addRole(String userId, String roleId) {
    User user = wingsPersistence.get(User.class, userId);
    Role role = wingsPersistence.get(Role.class, roleId);
    if (user != null && role != null) {
      UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).add("roles", role);
      Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userId);
      wingsPersistence.update(updateQuery, updateOp);
      return wingsPersistence.get(User.class, userId);
    }
    throw new WingsException(
        "Invalid operation. Either User or Role doesn't exist user = [" + user + "] role = [" + role + "]");
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#update(software.wings.beans.User)
   */
  public User update(User user) {
    Builder<String, Object> builder =
        ImmutableMap.<String, Object>builder().put("name", user.getName()).put("email", user.getEmail());
    if (user.getPassword() != null && user.getPassword().length() > 0) {
      builder.put("passwordHash", hashpw(user.getPassword(), BCrypt.gensalt()));
    }
    wingsPersistence.updateFields(User.class, user.getUuid(), builder.build());
    return wingsPersistence.get(User.class, user.getAppId(), user.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#list(software.wings.dl.PageRequest)
   */
  public PageResponse<User> list(PageRequest<User> pageRequest) {
    return wingsPersistence.query(User.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#delete(java.lang.String)
   */
  public void delete(String userId) {
    wingsPersistence.delete(User.class, userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#get(java.lang.String)
   */
  public User get(String userId) {
    return wingsPersistence.get(User.class, userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#revokeRole(java.lang.String, java.lang.String)
   */
  public User revokeRole(String userId, String roleId) {
    Role role = new Role();
    role.setUuid(roleId);
    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).removeAll("roles", role);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userId);
    wingsPersistence.update(updateQuery, updateOp);
    return wingsPersistence.get(User.class, userId);
  }
}
