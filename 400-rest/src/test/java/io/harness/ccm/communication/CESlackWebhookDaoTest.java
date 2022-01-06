/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.communication;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.ccm.communication.entities.CESlackWebhook.CESlackWebhookKeys;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
public class CESlackWebhookDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String slackWebhookUrl1 = "SLACK_WEBHOOK_URL_1";
  private String slackWebhookUrl2 = "SLACK_WEBHOOK_URL_2";
  private boolean sendCostReports1 = true;
  private boolean sendCostReports2 = false;
  private boolean sendAnomalyAlerts1 = false;
  private boolean sendAnomalyAlerts2 = true;

  private CESlackWebhook ceSlackWebhook1;
  private CESlackWebhook ceSlackWebhook2;
  @Inject private CESlackWebhookDao ceSlackWebhookDao;

  @Before
  public void setUp() {
    ceSlackWebhook1 = CESlackWebhook.builder()
                          .accountId(accountId)
                          .webhookUrl(slackWebhookUrl1)
                          .sendCostReport(sendCostReports1)
                          .sendAnomalyAlerts(sendAnomalyAlerts1)

                          .build();
    ceSlackWebhook2 = CESlackWebhook.builder()
                          .accountId(accountId)
                          .webhookUrl(slackWebhookUrl2)
                          .sendCostReport(sendCostReports2)
                          .sendAnomalyAlerts(sendAnomalyAlerts2)
                          .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsert() {
    CESlackWebhook actualCeSlackWebhook1 = ceSlackWebhookDao.upsert(ceSlackWebhook1);
    assertThat(actualCeSlackWebhook1)
        .isEqualToIgnoringGivenFields(actualCeSlackWebhook1, CESlackWebhookKeys.uuid, CESlackWebhookKeys.createdAt,
            CESlackWebhookKeys.lastUpdatedAt);
    CESlackWebhook actualCeSlackWebhook2 = ceSlackWebhookDao.upsert(ceSlackWebhook2);
    assertThat(actualCeSlackWebhook2.getUuid()).isEqualTo(actualCeSlackWebhook1.getUuid());
    assertThat(actualCeSlackWebhook2)
        .isEqualToIgnoringGivenFields(actualCeSlackWebhook2, CESlackWebhookKeys.uuid, CESlackWebhookKeys.createdAt,
            CESlackWebhookKeys.lastUpdatedAt);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    CESlackWebhook upsertedCESlackWebhook = ceSlackWebhookDao.upsert(ceSlackWebhook1);
    CESlackWebhook ceSlackWebhook = ceSlackWebhookDao.getByAccountId(upsertedCESlackWebhook.getAccountId());
    assertThat(ceSlackWebhook.getUuid()).isEqualTo(upsertedCESlackWebhook.getUuid());
  }
}
