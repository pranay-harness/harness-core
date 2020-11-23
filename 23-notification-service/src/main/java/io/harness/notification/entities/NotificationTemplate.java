package io.harness.notification.entities;

import io.harness.Team;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;

import com.google.common.collect.ImmutableList;
import java.util.List;
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
public class NotificationTemplate {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_identifier_team_idx")
                 .unique(true)
                 .field(TemplateKeys.identifier)
                 .field(TemplateKeys.team)
                 .build())
        .add(CompoundMongoIndex.builder().name("team_idx").field(TemplateKeys.team).build())
        .build();
  }
  @Id @org.mongodb.morphia.annotations.Id private String id;
  private String identifier;
  @CreatedDate private long createdAt;
  @Version private long version;
  @LastModifiedDate private long lastUpdatedAt;
  @Indexed private Team team;
  private byte[] file;
  private boolean harnessManaged;
}
