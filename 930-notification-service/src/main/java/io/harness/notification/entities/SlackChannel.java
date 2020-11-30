package io.harness.notification.entities;

import static io.harness.NotificationRequest.Slack;

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
@JsonTypeName("Slack")
public class SlackChannel implements Channel {
  List<String> slackWebHookUrls;
  List<String> userGroupIds;
  Map<String, String> templateData;
  @Override
  public Object toObjectofProtoSchema() {
    return Slack.newBuilder()
        .addAllSlackWebHookUrls(slackWebHookUrls)
        .addAllUserGroupIds(userGroupIds)
        .putAllTemplateData(templateData)
        .build();
  }

  @Override
  @JsonIgnore
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.SLACK;
  }

  public static SlackChannel toSlackEntity(Slack slackDetails) {
    return SlackChannel.builder()
        .slackWebHookUrls(slackDetails.getSlackWebHookUrlsList())
        .userGroupIds(slackDetails.getUserGroupIdsList())
        .templateData(slackDetails.getTemplateDataMap())
        .build();
  }
}
