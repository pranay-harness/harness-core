package io.harness.notification.entities;

import static io.harness.NotificationRequest.Slack;
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
@JsonTypeName("Slack")
public class SlackChannel implements Channel {
  List<String> slackWebHookUrls;
  List<UserGroup> userGroups;
  Map<String, String> templateData;
  @Override
  public Object toObjectofProtoSchema() {
    return Slack.newBuilder()
        .addAllSlackWebHookUrls(slackWebHookUrls)
        .putAllTemplateData(templateData)
        .addAllUserGroup(NotificationUserGroupMapper.toProto(userGroups))
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
        .templateData(slackDetails.getTemplateDataMap())
        .userGroups(NotificationUserGroupMapper.toEntity(slackDetails.getUserGroupList()))
        .build();
  }
}
