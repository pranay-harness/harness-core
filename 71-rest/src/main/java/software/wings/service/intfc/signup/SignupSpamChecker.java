package software.wings.service.intfc.signup;

import static software.wings.app.ManagerCacheRegistrar.TRIAL_EMAIL_CACHE;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.UserInvite;

import javax.cache.Cache;

@Slf4j
@Singleton
public class SignupSpamChecker {
  private static final int REGISTRATION_SPAM_THRESHOLD = 3;
  @Inject @Named(TRIAL_EMAIL_CACHE) private Cache<String, Integer> trialEmailCache;

  public boolean isSpam(UserInvite userInvite) {
    // HAR-7639: If the same email is being used repeatedly for trial signup, it's likely a spam activity.
    // Reject/throttle these registration request to avoid the verification or access-your-account email spamming
    // the legitimate trial user's mailbox.
    String emailAddress = userInvite.getEmail();
    Preconditions.checkNotNull(trialEmailCache, "Email cache is null. ");
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
}
