package io.harness.notification.entities;

import static io.harness.NotificationRequest.MSTeam;
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
@JsonTypeName("MicrosoftTeams")
public class MicrosoftTeamsChannel implements Channel {
  List<String> msTeamKeys;
  List<UserGroup> userGroups;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return MSTeam.newBuilder()
        .addAllMsTeamKeys(msTeamKeys)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
        .addAllUserGroup(NotificationUserGroupMapper.toProto(userGroups))
        .build();
  }

  @Override
  @JsonIgnore
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.MSTEAMS;
  }

  public static MicrosoftTeamsChannel toMicrosoftTeamsEntity(MSTeam msTeamDetails) {
    return MicrosoftTeamsChannel.builder()
        .msTeamKeys(msTeamDetails.getMsTeamKeysList())
        .templateData(msTeamDetails.getTemplateDataMap())
        .templateId(msTeamDetails.getTemplateId())
        .userGroups(NotificationUserGroupMapper.toEntity(msTeamDetails.getUserGroupList()))
        .build();
  }
}
