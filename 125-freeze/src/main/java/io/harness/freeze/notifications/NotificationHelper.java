/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.freeze.notifications;

import io.harness.beans.FeatureName;
import io.harness.freeze.beans.FreezeDuration;
import io.harness.freeze.beans.FreezeEvent;
import io.harness.freeze.beans.FreezeNotificationChannelWrapper;
import io.harness.freeze.beans.FreezeNotifications;
import io.harness.freeze.beans.response.FreezeSummaryResponseDTO;
import io.harness.freeze.beans.yaml.FreezeConfig;
import io.harness.freeze.beans.yaml.FreezeInfoConfig;
import io.harness.freeze.helpers.FreezeTimeUtils;
import io.harness.freeze.mappers.NGFreezeDtoMapper;
import io.harness.notification.FreezeEventType;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.utils.NGFeatureFlagHelperService;

import com.google.api.client.util.ArrayMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class NotificationHelper {
  @Inject NotificationClient notificationClient;
  @Inject private NGFeatureFlagHelperService ngFeatureFlagHelperService;

  public void sendNotification(String yaml, boolean pipelineRejectedNotification, boolean freezeWindowNotification,
      Ambiance ambiance, String accountId, String executionUrl, String baseUrl, boolean globalFreeze)
      throws IOException {
    if (!ngFeatureFlagHelperService.isEnabled(accountId, FeatureName.CDC_SEND_NOTIFICATION_FOR_FREEZE)) {
      return;
    }
    FreezeConfig freezeConfig = NGFreezeDtoMapper.toFreezeConfig(yaml);
    FreezeInfoConfig freezeInfoConfig = freezeConfig.getFreezeInfoConfig();
    if (freezeInfoConfig == null || freezeInfoConfig.getNotifications() == null) {
      return;
    }
    for (FreezeNotifications freezeNotifications : freezeInfoConfig.getNotifications()) {
      if (!freezeNotifications.isEnabled()) {
        continue;
      }
      FreezeNotificationChannelWrapper wrapper = freezeNotifications.getNotificationChannelWrapper().getValue();
      if (wrapper.getType() != null) {
        for (FreezeEvent freezeEvent : freezeNotifications.getEvents()) {
          String templateId = getNotificationTemplate(wrapper.getType(), freezeEvent);
          if (freezeEvent.getType().equals(FreezeEventType.FREEZE_WINDOW_ENABLED) && !freezeWindowNotification) {
            continue;
          }
          if (freezeEvent.getType().equals(FreezeEventType.DEPLOYMENT_REJECTED_DUE_TO_FREEZE)
              && !pipelineRejectedNotification) {
            continue;
          }
          Map<String, String> notificationContent = constructTemplateData(
              freezeEvent.getType(), freezeInfoConfig, ambiance, accountId, executionUrl, baseUrl, globalFreeze);
          NotificationChannel channel = wrapper.getNotificationChannel().toNotificationChannel(accountId,
              freezeInfoConfig.getOrgIdentifier(), freezeInfoConfig.getProjectIdentifier(), templateId,
              notificationContent, Ambiance.newBuilder().setExpressionFunctorToken(0).build());
          try {
            notificationClient.sendNotificationAsync(channel);
          } catch (Exception ex) {
            log.error("Failed to send notification ", ex);
          }
        }
      }
    }
  }

  private Map<String, String> constructTemplateData(FreezeEventType freezeEventType, FreezeInfoConfig freezeInfoConfig,
      Ambiance ambiance, String accountId, String executionUrl, String baseUrl, boolean globalFreeze) {
    Map<String, String> data = new ArrayMap<>();
    if (globalFreeze) {
      data.put("BLACKOUT_WINDOW_URL", getGlobalFreezeUrl(baseUrl, freezeInfoConfig, accountId));
    } else {
      data.put("BLACKOUT_WINDOW_URL", getManualFreezeUrl(baseUrl, freezeInfoConfig, accountId));
    }
    data.put("BLACKOUT_WINDOW_NAME", freezeInfoConfig.getName());
    if (freezeInfoConfig.getWindows().size() > 0) {
      TimeZone timeZone = TimeZone.getTimeZone(freezeInfoConfig.getWindows().get(0).getTimeZone());
      LocalDateTime firstWindowStartTime =
          LocalDateTime.parse(freezeInfoConfig.getWindows().get(0).getStartTime(), FreezeTimeUtils.dtf);
      LocalDateTime firstWindowEndTime;
      if (freezeInfoConfig.getWindows().get(0).getEndTime() == null) {
        FreezeDuration freezeDuration = FreezeDuration.fromString(freezeInfoConfig.getWindows().get(0).getDuration());
        Long endTime = FreezeTimeUtils.getEpochValueFromDateString(firstWindowStartTime, timeZone)
            + freezeDuration.getTimeoutInMillis();
        firstWindowEndTime = Instant.ofEpochMilli(endTime).atZone(timeZone.toZoneId()).toLocalDateTime();
      } else {
        firstWindowEndTime =
            LocalDateTime.parse(freezeInfoConfig.getWindows().get(0).getEndTime(), FreezeTimeUtils.dtf);
      }
      Pair<LocalDateTime, LocalDateTime> windowTimes = Pair.of(firstWindowStartTime, firstWindowEndTime);
      if (freezeInfoConfig.getWindows().get(0).getRecurrence() != null
          && freezeInfoConfig.getWindows().get(0).getRecurrence().getRecurrenceType() != null) {
        windowTimes = FreezeTimeUtils.setCurrWindowStartAndEndTime(firstWindowStartTime, firstWindowEndTime,
            freezeInfoConfig.getWindows().get(0).getRecurrence().getRecurrenceType(), timeZone);
      }
      data.put("START_TIME", windowTimes.getLeft().toString());
      data.put("END_TIME", windowTimes.getRight().toString());
      data.put("ACCOUNT_ID", accountId);
    }
    if (freezeEventType.equals(FreezeEventType.DEPLOYMENT_REJECTED_DUE_TO_FREEZE) && ambiance != null) {
      data.put("USER_NAME", ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getIdentifier());
      data.put("WORKFLOW_NAME", ambiance.getMetadata().getPipelineIdentifier());
      data.put("WORKFLOW_URL", executionUrl);
    }
    return data;
  }

  private String getNotificationTemplate(String channelType, FreezeEvent freezeEvent) {
    if (freezeEvent.getType().equals(FreezeEventType.DEPLOYMENT_REJECTED_DUE_TO_FREEZE)) {
      return String.format("pipeline_rejected_%s_alert", channelType.toLowerCase());
    }
    return String.format("freeze_%s_alert", channelType.toLowerCase());
  }

  public void sendNotificationForFreezeConfigs(List<FreezeSummaryResponseDTO> manualFreezeConfigs,
      List<FreezeSummaryResponseDTO> globalFreezeConfigs, Ambiance ambiance, String executionUrl, String baseUrl) {
    for (FreezeSummaryResponseDTO freezeSummaryResponseDTO : globalFreezeConfigs) {
      if (freezeSummaryResponseDTO.getYaml() != null) {
        try {
          sendNotification(freezeSummaryResponseDTO.getYaml(), true, false, ambiance,
              freezeSummaryResponseDTO.getAccountId(), executionUrl, baseUrl, true);
        } catch (Exception e) {
          log.info("Unable to send pipeline rejected notifications for global freeze", e);
        }
      }
    }
    for (FreezeSummaryResponseDTO freezeSummaryResponseDTO : manualFreezeConfigs) {
      if (freezeSummaryResponseDTO.getYaml() != null) {
        try {
          sendNotification(freezeSummaryResponseDTO.getYaml(), true, false, ambiance,
              freezeSummaryResponseDTO.getAccountId(), executionUrl, baseUrl, false);
        } catch (Exception e) {
          log.info("Unable to send pipeline rejected notifications for manual freeze", e);
        }
      }
    }
  }

  private String getManualFreezeUrl(String baseUrl, FreezeInfoConfig freezeInfoConfig, String accountId) {
    String freezeUrl = "";
    if (freezeInfoConfig != null) {
      String orgId = freezeInfoConfig.getOrgIdentifier();
      String projectId = freezeInfoConfig.getProjectIdentifier();
      if (orgId != null) {
        if (projectId != null) {
          freezeUrl = String.format("%s/account/%s/cd/orgs/%s/projects/%s/setup/freeze-window-studio/window/%s",
              baseUrl, accountId, orgId, projectId, freezeInfoConfig.getIdentifier());
        } else {
          freezeUrl = String.format("%s/account/%s/settings/organizations/%s/setup/freeze-window-studio/window/%s",
              baseUrl, accountId, orgId, freezeInfoConfig.getIdentifier());
        }
      } else {
        freezeUrl = String.format("%s/account/%s/settings/freeze-window-studio/window/%s", baseUrl, accountId,
            freezeInfoConfig.getIdentifier());
      }
    }
    return freezeUrl;
  }

  private String getGlobalFreezeUrl(String baseUrl, FreezeInfoConfig freezeInfoConfig, String accountId) {
    String freezeUrl = "";
    if (freezeInfoConfig != null) {
      String orgId = freezeInfoConfig.getOrgIdentifier();
      String projectId = freezeInfoConfig.getProjectIdentifier();
      if (orgId != null) {
        if (projectId != null) {
          freezeUrl = String.format(
              "%s/account/%s/cd/orgs/%s/projects/%s/setup/freeze-windows", baseUrl, accountId, orgId, projectId);
        } else {
          freezeUrl =
              String.format("%s/account/%s/settings/organizations/%s/setup/freeze-windows", baseUrl, accountId, orgId);
        }
      } else {
        freezeUrl = String.format("%s/account/%s/settings/freeze-windows", baseUrl, accountId);
      }
    }
    return freezeUrl;
  }
}
