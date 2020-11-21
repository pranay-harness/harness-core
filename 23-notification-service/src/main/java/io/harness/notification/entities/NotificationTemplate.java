package io.harness.notification.entities;

import io.harness.Team;
import io.harness.mongo.index.*;
import io.harness.notification.entities.NotificationTemplate.TemplateKeys;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@FieldNameConstants(innerTypeName = "TemplateKeys")
@Document("notificationTemplates")
@Entity("notificationTemplates")
@TypeAlias("notificationTemplate")
@NgUniqueIndex(
    name = "unique_identifier_team_idx", fields = { @Field(TemplateKeys.identifier)
                                                    , @Field(TemplateKeys.team) })
@CdIndex(name = "team_idx", fields = { @Field(TemplateKeys.team) })
public class NotificationTemplate {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  private String identifier;
  @CreatedDate private long createdAt;
  @Version private long version;
  @LastModifiedDate private long lastUpdatedAt;
  @Indexed private Team team;
  private byte[] file;
  private boolean harnessManaged;
}
