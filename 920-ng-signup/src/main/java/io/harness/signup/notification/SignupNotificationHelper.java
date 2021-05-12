package io.harness.signup.notification;

import io.harness.Team;
import io.harness.exception.SignupException;
import io.harness.ng.core.user.UserInfo;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.EmailChannel.EmailChannelBuilder;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.remote.client.RestClientUtils;
import io.harness.signup.SignupNotificationConfiguration;
import io.harness.user.remote.UserClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class SignupNotificationHelper {
  private final UserClient userClient;
  private final NotificationClient notificationClient;
  private final SignupNotificationConfiguration notificationConfiguration;
  private final LoadingCache<EmailType, Boolean> cache;

  @Inject
  public SignupNotificationHelper(UserClient userClient, NotificationClient notificationClient,
      SignupNotificationTemplateLoader cacheLoader, SignupNotificationConfiguration notificationConfiguration) {
    this.userClient = userClient;
    this.notificationClient = notificationClient;
    this.notificationConfiguration = notificationConfiguration;
    cache = CacheBuilder.newBuilder()
                .maximumSize(notificationConfiguration.getTemplates().size())
                .expireAfterWrite(notificationConfiguration.getExpireDurationInMinutes(), TimeUnit.MINUTES)
                .build(cacheLoader);
  }

  public void sendSignupNotification(UserInfo userInfo, EmailType emailType, String defaultTemplateId) {
    String url = generateUrl(emailType, userInfo);
    String templateId = decideTemplateId(emailType, defaultTemplateId);

    EmailChannelBuilder builder = EmailChannel.builder()
                                      .accountId(userInfo.getDefaultAccountId())
                                      .team(Team.GTM)
                                      .recipients(Lists.newArrayList(userInfo.getEmail()))
                                      .templateId(templateId)
                                      .templateData(ImmutableMap.of("name", userInfo.getName(), "url", url))
                                      .userGroupIds(Collections.emptyList());
    notificationClient.sendNotificationAsync(builder.build());
  }

  private String generateUrl(EmailType emailType, UserInfo userInfo) {
    // TODO: for EmailType.CONFIRM needs to generate an url to NG login page
    Optional<String> urlOptional =
        RestClientUtils.getResponse(userClient.generateSignupNotificationUrl(emailType.name().toLowerCase(), userInfo));
    if (!urlOptional.isPresent()) {
      throw new SignupException(
          String.format("Failed to generate verification url for user [%s] during signup", userInfo.getEmail()));
    }
    return urlOptional.get();
  }

  private String decideTemplateId(EmailType emailType, String defaultTemplateId) {
    String currentId = defaultTemplateId;
    try {
      EmailInfo emailInfo = notificationConfiguration.getTemplates().get(emailType);
      if (cache.get(emailType)) {
        currentId = emailInfo.getTemplateId();
      } else {
        log.error("Template id {} is not saved in notification service, use {} instead.", emailInfo.getTemplateId(),
            defaultTemplateId);
      }
    } catch (ExecutionException e) {
      log.error(
          String.format("Failed to get template %s from cache, fall over to use %s", emailType, defaultTemplateId), e);
    }
    return currentId;
  }
}
