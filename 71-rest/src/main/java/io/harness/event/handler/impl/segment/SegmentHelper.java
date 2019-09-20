package io.harness.event.handler.impl.segment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.event.model.EventConstants.ACCOUNT_STATUS;
import static io.harness.event.model.EventConstants.COMPANY_NAME;
import static io.harness.event.model.EventConstants.DAYS_LEFT_IN_TRIAL;
import static io.harness.event.model.EventConstants.FIRST_NAME;
import static io.harness.event.model.EventConstants.LAST_NAME;
import static io.harness.event.model.EventConstants.OAUTH_PROVIDER;
import static io.harness.event.model.EventConstants.USER_INVITE_URL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.IdentifyMessage.Builder;
import com.segment.analytics.messages.TrackMessage;
import io.harness.data.structure.UUIDGenerator;
import io.harness.event.handler.impl.Utils;
import io.harness.event.model.EventConstants;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 11/20/18
 */
@Singleton
@Slf4j
public class SegmentHelper {
  @Inject private Utils utils;

  public String createOrUpdateIdentity(String apiKey, String userId, String email, String userName, Account account,
      String userInviteUrl, String oauthProvider) throws IOException {
    logger.info("Updating identity {} to segment", userId);

    Builder identityBuilder = IdentifyMessage.builder();

    String identity;
    if (isEmpty(userId)) {
      identity = UUIDGenerator.generateUuid();
      identityBuilder.anonymousId(identity);
    } else {
      identity = userId;
      identityBuilder.userId(identity);
    }

    Map<String, String> traits = new HashMap<>();
    traits.put("email", email);
    traits.put(FIRST_NAME, utils.getFirstName(userName, email));
    String lastName = utils.getLastName(userName, email);
    traits.put(LAST_NAME, isNotEmpty(lastName) ? lastName : "");

    if (account != null) {
      traits.put(COMPANY_NAME, account.getCompanyName());
      traits.put(EventConstants.ACCOUNT_ID, account.getUuid());

      LicenseInfo licenseInfo = account.getLicenseInfo();
      if (licenseInfo != null) {
        traits.put(ACCOUNT_STATUS, licenseInfo.getAccountStatus());
        traits.put(DAYS_LEFT_IN_TRIAL, utils.getDaysLeft(licenseInfo.getExpiryTime()));
      }
    }

    if (isNotEmpty(userInviteUrl)) {
      traits.put(USER_INVITE_URL, userInviteUrl);
    }

    if (isNotEmpty(oauthProvider)) {
      traits.put(OAUTH_PROVIDER, oauthProvider);
    }

    identityBuilder.traits(traits);

    Analytics analytics = Analytics.builder(apiKey).build();
    analytics.enqueue(identityBuilder);
    logger.info("Updated identity {} to segment", identity);
    return identity;
  }

  public boolean reportTrackEvent(String apiKey, String identity, String event, Map<String, String> properties) {
    Analytics analytics = Analytics.builder(apiKey).build();

    TrackMessage.Builder track = TrackMessage.builder(event).properties(properties).userId(identity);
    analytics.enqueue(track);
    return true;
  }

  public void reportTrackEvent(String apiKey, List<String> userIds, String event, Map<String, String> properties) {
    userIds.forEach(userId -> reportTrackEvent(apiKey, userId, event, properties));
  }
}
