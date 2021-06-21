package io.harness.ng.accesscontrol.migrations.models;

import static io.harness.ng.DbAliases.NG_MANAGER;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;

import com.google.common.collect.ImmutableList;
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
@FieldNameConstants(innerTypeName = "AccessControlMigrationKeys")
@Document("accessControlMigrations")
@Entity(value = "accessControlMigrations", noClassnameStored = true)
@TypeAlias("accessControlMigrations")
@StoreIn(NG_MANAGER)
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigration {
  @Id @org.springframework.data.annotation.Id String id;
  @FdIndex String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  private long createdRoleAssignments;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountIdentifierOrgIdentifierProjectIdentifierIdx")
                 .field(AccessControlMigrationKeys.accountIdentifier)
                 .field(AccessControlMigrationKeys.orgIdentifier)
                 .field(AccessControlMigrationKeys.projectIdentifier)
                 .unique(true)
                 .build())
        .build();
  }
}
