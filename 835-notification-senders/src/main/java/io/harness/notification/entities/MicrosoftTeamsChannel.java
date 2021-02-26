package io.harness.notification.entities;

import static io.harness.NotificationRequest.MSTeam;

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
@JsonTypeName("MicrosoftTeams")
public class MicrosoftTeamsChannel implements Channel {
  List<String> msTeamKeys;
  List<String> userGroupIds;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return MSTeam.newBuilder()
        .addAllMsTeamKeys(msTeamKeys)
        .addAllUserGroupIds(userGroupIds)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
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
        .userGroupIds(msTeamDetails.getUserGroupIdsList())
        .templateData(msTeamDetails.getTemplateDataMap())
        .templateId(msTeamDetails.getTemplateId())
        .build();
  }
}
