/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
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
@StoreIn(DbAliases.NOTIFICATION)
@OwnedBy(PL)
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
  boolean sendNotificationViaDelegate;
  SmtpConfig smtpConfig;
  @Version Long version;
}
