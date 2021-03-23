package io.harness.accesscontrol.migrations.models;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;

import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "MigrationKeys")
@Document("migrations")
@Entity(value = "migrations", noClassnameStored = true)
@TypeAlias("migrations")
@StoreIn(ACCESS_CONTROL)
public class Migration {
  @Id @org.springframework.data.annotation.Id String id;
  private Date startedAt;
  private Date endedAt;
  private List<UserMigration> userMigrations;
  @FdIndex private String accountId;
}
