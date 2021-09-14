/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.entities;

import static io.harness.NotificationRequest.PagerDuty;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.dtos.UserGroup;
import io.harness.notification.mapper.NotificationUserGroupMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode()
@JsonTypeName("PagerDuty")
public class PagerDutyChannel implements Channel {
  List<String> pagerDutyIntegrationKeys;
  List<UserGroup> userGroups;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return PagerDuty.newBuilder()
        .addAllPagerDutyIntegrationKeys(pagerDutyIntegrationKeys)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
        .addAllUserGroup(NotificationUserGroupMapper.toProto(userGroups))
        .build();
  }

  @Override
  @JsonIgnore
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.PAGERDUTY;
  }

  public static PagerDutyChannel toPagerDutyEntity(PagerDuty pagerDutyDetails) {
    return PagerDutyChannel.builder()
        .pagerDutyIntegrationKeys(pagerDutyDetails.getPagerDutyIntegrationKeysList())
        .templateData(pagerDutyDetails.getTemplateDataMap())
        .templateId(pagerDutyDetails.getTemplateId())
        .userGroups(NotificationUserGroupMapper.toEntity(pagerDutyDetails.getUserGroupList()))
        .build();
  }
}
