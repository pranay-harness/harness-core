package io.harness.event.handler.impl.segment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.UUIDGenerator;
import io.harness.event.handler.impl.Utils;
import io.harness.event.handler.segment.SegmentRestClient;
import io.harness.event.model.segment.Identity;
import io.harness.event.model.segment.Identity.IdentityBuilder;
import io.harness.event.model.segment.Properties;
import io.harness.event.model.segment.Response;
import io.harness.event.model.segment.Trace;
import io.harness.event.model.segment.Traits;
import io.harness.event.model.segment.Traits.TraitsBuilder;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/20/18
 */
@Singleton
@Slf4j
public class SegmentHelper {
  @Inject private Utils utils;

  public String createOrUpdateIdentity(String apiKey, Retrofit retrofit, String userId, String email, String userName,
      Account account, String userInviteUrl, String oauthProvider) throws IOException {
    logger.info("Updating identity {} to segment", userId);

    IdentityBuilder identityBuilder = Identity.builder();
    String identity;
    if (isEmpty(userId)) {
      identity = UUIDGenerator.generateUuid();
      identityBuilder.anonymousId(identity);
    } else {
      identity = userId;
      identityBuilder.userId(identity);
    }

    TraitsBuilder traitsBuilder = Traits.builder()
                                      .email(email)
                                      .firstName(utils.getFirstName(userName, email))
                                      .lastName(utils.getLastName(userName, email));

    if (account != null) {
      traitsBuilder.companyName(account.getCompanyName()).accountId(account.getUuid());

      LicenseInfo licenseInfo = account.getLicenseInfo();
      if (licenseInfo != null) {
        traitsBuilder.accountStatus(licenseInfo.getAccountStatus())
            .daysLeftInTrial(utils.getDaysLeft(licenseInfo.getExpiryTime()));
      }
    }

    if (isNotEmpty(userInviteUrl)) {
      traitsBuilder.userInviteUrl(userInviteUrl);
    }

    if (isNotEmpty(oauthProvider)) {
      traitsBuilder.oauthProvider(oauthProvider);
    }

    identityBuilder.traits(traitsBuilder.build());

    retrofit2.Response<Response> response =
        retrofit.create(SegmentRestClient.class).identity(apiKey, identityBuilder.build()).execute();
    if (response.body().isSuccess()) {
      logger.info("Updated identity {} to segment", identity);
    } else {
      logger.error("Failed while updating identity {} to segment", identity);
    }
    return identity;
  }

  public boolean reportTrackEvent(String apiKey, Retrofit retrofit, String identity, String event) {
    return reportTrackEvent(apiKey, retrofit, identity, event, String.valueOf(System.currentTimeMillis()));
  }

  private boolean reportTrackEvent(String apiKey, Retrofit retrofit, String identity, String event, String timestamp) {
    Properties properties = Properties.builder().original_timestamp(timestamp).build();
    Trace trace = Trace.builder().event(event).userId(identity).properties(properties).build();
    try {
      retrofit2.Response<Response> response = retrofit.create(SegmentRestClient.class).track(apiKey, trace).execute();
      if (response.body().isSuccess()) {
        logger.info("Reported event {} to segment for user {}", event, identity);
        return true;
      } else {
        logger.error("Failed to report event {} to segment for user {}", event, identity);
        return false;
      }
    } catch (IOException e) {
      logger.error("Error while sending event {} to segment for user {}", event, identity, e);
      return false;
    }
  }

  public void reportTrackEvent(String apiKey, Retrofit retrofit, List<String> userIds, String event) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    userIds.forEach(userId -> reportTrackEvent(apiKey, retrofit, userId, event, timestamp));
  }
}
