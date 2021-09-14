/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.channelDetails;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.notification.channeldetails.MSTeamChannel;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsMsTeamsChannelTest extends CategoryTest {
  List<String> userGroups = new ArrayList<>();
  List<String> msTeamsKeys = Collections.singletonList("key1");
  PmsMSTeamChannel pmsMSTeamChannel;
  @Before
  public void setUp() {
    userGroups.add("user");
    userGroups.add("org.user");
    pmsMSTeamChannel = spy(PmsMSTeamChannel.builder().msTeamKeys(msTeamsKeys).userGroups(userGroups).build());
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testToNotificationChannel() {
    String accountId = "accountId";
    String orgId = "orgId";
    String projectId = "projectId";
    String templateId = "temolateId";
    Map<String, String> templateData = new HashMap<>();
    NotificationChannel notificationChannel =
        pmsMSTeamChannel.toNotificationChannel(accountId, orgId, projectId, templateId, templateData);
    assertEquals(notificationChannel.getAccountId(), accountId);
    assertEquals(((MSTeamChannel) notificationChannel).getMsTeamKeys(), msTeamsKeys);
    assertEquals(notificationChannel.getUserGroups().get(0).getOrgIdentifier(), orgId);
    assertEquals(notificationChannel.getUserGroups().get(0).getProjectIdentifier(), projectId);

    assertEquals(notificationChannel.getUserGroups().get(1).getOrgIdentifier(), orgId);
    assertEquals(notificationChannel.getUserGroups().get(1).getProjectIdentifier(), "");
  }
}
