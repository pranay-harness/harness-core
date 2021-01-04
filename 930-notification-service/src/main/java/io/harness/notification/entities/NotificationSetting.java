package io.harness.notification.entities;

import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.notification.SmtpConfig;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NotificationSettingKeys")
@Entity(value = "notificationSettings")
@Document("notificationSettings")
@TypeAlias("notificationSettings")
public class NotificationSetting {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_notificationsetting_idx")
                 .unique(true)
                 .field(NotificationSettingKeys.accountId)
                 .build())
        .build();
  }
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountId;
  @Builder.Default boolean sendNotificationViaDelegate = false;
  SmtpConfig smtpConfig;
  @Version Long version;
}
