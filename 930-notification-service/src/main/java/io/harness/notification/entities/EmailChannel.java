package io.harness.notification.entities;

import static io.harness.NotificationRequest.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode()
@JsonTypeName("Email")
public class EmailChannel implements Channel {
  List<String> emailIds;
  List<String> userGroupIds;
  Map<String, String> templateData;
  String templateId;

  @Override
  public Object toObjectofProtoSchema() {
    return Email.newBuilder()
        .addAllEmailIds(emailIds)
        .addAllUserGroupIds(userGroupIds)
        .putAllTemplateData(templateData)
        .setTemplateId(templateId)
        .build();
  }

  public static EmailChannel toEmailEntity(Email emailDetails) {
    return EmailChannel.builder()
        .emailIds(emailDetails.getEmailIdsList())
        .userGroupIds(emailDetails.getUserGroupIdsList())
        .templateData(emailDetails.getTemplateDataMap())
        .templateId(emailDetails.getTemplateId())
        .build();
  }
}
