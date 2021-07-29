package io.harness.notification.entities;

import static io.harness.NotificationRequest.Email;
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
@JsonTypeName("Email")
public class EmailChannel implements Channel {
  List<String> emailIds;
  List<String> userGroupIds;
  List<UserGroup> userGroups;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return Email.newBuilder()
        .addAllEmailIds(emailIds)
        .addAllUserGroupIds(userGroupIds)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
        .addAllUserGroup(NotificationUserGroupMapper.toProto(userGroups))
        .build();
  }

  @Override
  @JsonIgnore
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.EMAIL;
  }

  public static EmailChannel toEmailEntity(Email emailDetails) {
    return EmailChannel.builder()
        .emailIds(emailDetails.getEmailIdsList())
        .userGroupIds(emailDetails.getUserGroupIdsList())
        .templateData(emailDetails.getTemplateDataMap())
        .templateId(emailDetails.getTemplateId())
        .userGroups(NotificationUserGroupMapper.toEntity(emailDetails.getUserGroupList()))
        .build();
  }
}
