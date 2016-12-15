package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 12/14/16.
 */

@Singleton
public class SlackNotificationServiceImpl implements SlackNotificationService {
  @Inject private SettingsService settingsService;

  @Override
  public void sendMessage(String slackConfigId, String slackChanel, String message) {}
}
