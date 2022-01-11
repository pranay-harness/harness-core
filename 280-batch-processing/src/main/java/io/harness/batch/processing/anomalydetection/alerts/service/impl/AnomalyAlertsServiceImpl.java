/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.anomalydetection.alerts.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.slack.api.webhook.WebhookPayloads.payload;

import io.harness.batch.processing.anomalydetection.alerts.SlackMessageGenerator;
import io.harness.batch.processing.anomalydetection.alerts.service.itfc.AnomalyAlertsService;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CESlackWebhook;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.webhook.WebhookResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class AnomalyAlertsServiceImpl implements AnomalyAlertsService {
  @Autowired @Inject private AnomalyService anomalyService;
  @Autowired @Inject private CESlackWebhookService ceSlackWebhookService;
  @Autowired @Inject private AccountShardService accountShardService;
  @Autowired @Inject private SlackMessageGenerator slackMessageGenerator;
  @Autowired @Inject private Slack slack;

  int MAX_RETRY = 3;

  public void sendAnomalyDailyReport(String accountId, Instant date) {
    if (slack == null) {
      slack = Slack.getInstance();
    }
    try {
      checkAndSendDailyReport(accountId, date);
    } catch (Exception e) {
      log.error("Can't send daily report for account : {}, Exception: ", accountId, e);
    }
  }

  private void checkAndSendDailyReport(String accountId, Instant date) {
    checkNotNull(accountId);
    CESlackWebhook slackWebhook = ceSlackWebhookService.getByAccountId(accountId);
    if (slackWebhook == null) {
      log.warn("The Account with id={} has no associated communication channels to send anomaly alerts.", accountId);
      return;
    }
    if (!slackWebhook.isSendAnomalyAlerts()) {
      log.info("The Account with id={} has anomaly alerts turned off", accountId);
      return;
    }
    try {
      sendDailyReportViaSlack(slackWebhook, date);
    } catch (IOException | SlackApiException e) {
      log.error("Unable to send slack daily notification  for account : [{}] Exception : [{}]", accountId, e);
    }
  }

  private void sendDailyReportViaSlack(CESlackWebhook slackWebhook, Instant date)
      throws IOException, SlackApiException {
    String accountId = slackWebhook.getAccountId();
    List<AnomalyEntity> anomalies = anomalyService.list(accountId, date);
    List<LayoutBlock> layoutBlocks;
    if (!anomalies.isEmpty()) {
      layoutBlocks = slackMessageGenerator.generateDailyReport(anomalies);
      int count = 0;
      while (count < MAX_RETRY) {
        try {
          WebhookResponse response = slack.send(
              slackWebhook.getWebhookUrl(), payload(p -> p.text("Harness CE Anomaly Alert").blocks(layoutBlocks)));
          count++;
          if (response.getCode() == 200) {
            log.info("slack daily anomalies notification sent successfully for accountId : {} ", accountId);
            break;
          }
        } catch (Exception e) {
          log.error("could not send daily anomalies notification via slack for accountId : {} , Exception : {} ",
              accountId, e);
        }
      }
    }
  }
}
