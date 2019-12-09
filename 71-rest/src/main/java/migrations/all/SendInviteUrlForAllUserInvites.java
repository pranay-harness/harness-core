package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.event.handler.impl.MarketoHelper;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.network.Http;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.io.IOException;

/**
 * Migration script to set inviteUrl to marketo leads for all existing pending user invites.
 * @author rktummala on 02/22/19
 */
@Slf4j
public class SendInviteUrlForAllUserInvites implements Migration {
  @Inject private UserService userService;
  @Inject private AccountService accountService;
  @Inject private MarketoHelper marketoHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public void migrate() {
    logger.info("Start - Setting inviteURL for users");
    MarketoConfig marketoConfig = mainConfiguration.getMarketoConfig();

    if (!marketoConfig.isEnabled()) {
      logger.info("Marketo is disabled, skipping the migration");
      return;
    }

    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(marketoConfig.getUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .client(Http.getUnsafeOkHttpClient(marketoConfig.getUrl()))
                            .build();

    logger.info("MarketoMigration - Start - registering all users of trial accounts as leads");
    String accessToken;
    try {
      accessToken =
          marketoHelper.getAccessToken(marketoConfig.getClientId(), marketoConfig.getClientSecret(), retrofit);
      logger.info("Obtained access token");
    } catch (IOException e) {
      logger.error("Error while trying to get access token in migration", e);
      return;
    }

    try (HIterator<UserInvite> userInvites = new HIterator<>(wingsPersistence.createQuery(UserInvite.class).fetch())) {
      UserInvite userInvite;
      User user = null;
      while (userInvites.hasNext()) {
        try {
          userInvite = userInvites.next();

          if (userInvite.isCompleted()) {
            continue;
          }

          if (isEmpty(userInvite.getAccountId())) {
            marketoHelper.createOrUpdateLead(null, null, userInvite.getEmail(), accessToken, null, retrofit, null);
          } else {
            Account account = accountService.get(userInvite.getAccountId());
            if (account != null && account.getLicenseInfo() != null
                && AccountType.TRIAL.equals(account.getLicenseInfo().getAccountType())) {
              user = userService.getUserByEmail(userInvite.getEmail(), userInvite.getAccountId());
              if (user != null) {
                marketoHelper.createOrUpdateLead(
                    account, user.getName(), user.getEmail(), accessToken, user.getOauthProvider(), retrofit, null);
              }
            }
          }
        } catch (Exception ex) {
          logger.error("Error while setting inviteURL for user {}", user == null ? "NA" : user.getName(), ex);
        }
      }
    }
    logger.info("End - Setting inviteURL for users");
  }
}
