package software.wings.scheduler;

import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.CronScheduleBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.PasswordExpirationPolicy;
import software.wings.dl.WingsPersistence;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Have to add the password expiration double check case.
 */
@Slf4j
@DisallowConcurrentExecution
public class AccountPasswordExpirationJob implements Job {
  public static final String NAME = "LOGIN_SETTINGS_CRON";
  public static final String GROUP = "PASSWORD_POLICY_EXPIRATION";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserServiceImpl userService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AccountService accountService;
  @Inject private LoginSettingsService loginSettingsService;

  public static void add(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(AccountPasswordExpirationJob.class).withIdentity(NAME, GROUP).build();

    // the job should run daily at 12 hours 0 mins.
    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(NAME, GROUP)
                          .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(12, 0))
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    checkAccountPasswordExpiration();
  }

  public void checkAccountPasswordExpiration() {
    try (HIterator<User> userIterator =
             new HIterator<>(wingsPersistence.createQuery(User.class, excludeAuthority).fetch())) {
      Account account;
      for (User user : userIterator) {
        try {
          account = accountService.get(user.getDefaultAccountId());
          LoginSettings loginSettings = loginSettingsService.getLoginSettings(account.getUuid());
          PasswordExpirationPolicy passwordExpirationPolicy = loginSettings.getPasswordExpirationPolicy();

          // Mails will only be sent if the login mechanism of the account is USER_PASSWORD.
          if (account.getAuthenticationMechanism() == null
              || account.getAuthenticationMechanism() == AuthenticationMechanism.USER_PASSWORD) {
            if (!passwordExpirationPolicy.isEnabled()) {
              continue;
            }
            checkForPasswordExpiration(passwordExpirationPolicy, user);
          } else {
            logger.info(
                "Skipping AccountPasswordExpirationCheckJob for accountId {} because auth mechanism is not User password",
                account.getUuid());
          }
        } catch (Exception ex) {
          logger.error("CheckAccountPasswordExpiration failed for User: {}", user.getEmail(), ex);
        }
      }
    } catch (Exception ex) {
      logger.error("Error while running AccountPasswordExpirationCheckJob", ex);
    }
  }

  private void checkForPasswordExpiration(PasswordExpirationPolicy passwordExpirationPolicy, User user) {
    logger.info("AccountPasswordExpirationJob: processing user: {}", user.getEmail());
    long passwordChangedAt = user.getPasswordChangedAt();

    // for someone who has never changed his password, this value will be 0.
    if (passwordChangedAt == 0) {
      passwordChangedAt = user.getCreatedAt();
    }

    long passwordAgeInDays = Instant.ofEpochMilli(passwordChangedAt).until(Instant.now(), ChronoUnit.DAYS);
    if (hasPasswordExpired(passwordAgeInDays, passwordExpirationPolicy)) {
      markPasswordAsExpired(user);
      userService.sendPasswordExpirationMail(user.getEmail());
    } else if (isPasswordAboutToExpire(passwordAgeInDays, passwordExpirationPolicy)) {
      userService.sendPasswordExpirationWarning(
          user.getEmail(), passwordExpirationPolicy.getDaysBeforeUserNotifiedOfPasswordExpiration());
    }
  }

  private boolean hasPasswordExpired(long passwordAgeInDays, PasswordExpirationPolicy passwordExpirationPolicy) {
    return passwordAgeInDays == passwordExpirationPolicy.getDaysBeforePasswordExpires();
  }

  private boolean isPasswordAboutToExpire(long passwordAgeInDays, PasswordExpirationPolicy passwordExpirationPolicy) {
    long passwordAgeForSendingWaringMail = passwordExpirationPolicy.getDaysBeforePasswordExpires()
        - passwordExpirationPolicy.getDaysBeforeUserNotifiedOfPasswordExpiration() - 1;
    return passwordAgeInDays == passwordAgeForSendingWaringMail;
  }

  private void markPasswordAsExpired(User user) {
    UpdateOperations<User> operations = wingsPersistence.createUpdateOperations(User.class);
    setUnset(operations, "passwordExpired", true);
    update(user, operations);
  }

  private void update(User user, UpdateOperations<User> operations) {
    Query<User> query = wingsPersistence.createQuery(User.class).filter(ID_KEY, user.getUuid());
    wingsPersistence.update(query, operations);
  }
}
