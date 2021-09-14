/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.remote.resources;

import static io.harness.rule.OwnerRule.ANKUSH;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.remote.dto.SlackSettingDTO;
import io.harness.notification.service.api.ChannelService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ChannelResourceTest extends CategoryTest {
  @Mock private ChannelService channelService;

  private ChannelResource channelResource;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    channelResource = new ChannelResource(channelService);
  }

  @Test
  @Owner(developers = ANKUSH)
  @Category(UnitTests.class)
  public void testNotificationSetting_NotificationIdAbsent() {
    NotificationSettingDTO notificationSettingDTO =
        SlackSettingDTO.builder().recipient("slack-webhook").accountId("kmpySmUISimoRrJL6NL73w").build();
    channelResource.testNotificationSetting(notificationSettingDTO);
    verify(channelService, times(1)).sendTestNotification(notificationSettingDTO);
  }
}
