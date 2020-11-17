package io.harness.notification.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

import static io.harness.NotificationRequest.*;

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

  public static SlackChannel toSlackEntity(Slack slackDetails) {
    return SlackChannel.builder()
        .slackWebHookUrls(slackDetails.getSlackWebHookUrlsList())
        .userGroupIds(slackDetails.getUserGroupIdsList())
        .templateData(slackDetails.getTemplateDataMap())
        .build();
  }
}
