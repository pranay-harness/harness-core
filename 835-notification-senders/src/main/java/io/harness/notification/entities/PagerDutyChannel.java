package io.harness.notification.entities;

import static io.harness.NotificationRequest.PagerDuty;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode()
@JsonTypeName("PagerDuty")
public class PagerDutyChannel implements Channel {
  List<String> pagerDutyIntegrationKeys;
  List<String> userGroupIds;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return PagerDuty.newBuilder()
        .addAllPagerDutyIntegrationKeys(pagerDutyIntegrationKeys)
        .addAllUserGroupIds(userGroupIds)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
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
        .userGroupIds(pagerDutyDetails.getUserGroupIdsList())
        .templateData(pagerDutyDetails.getTemplateDataMap())
        .templateId(pagerDutyDetails.getTemplateId())
        .build();
  }
}
