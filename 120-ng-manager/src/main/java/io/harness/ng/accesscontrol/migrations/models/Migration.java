package io.harness.ng.accesscontrol.migrations.models;

import static io.harness.ng.DbAliases.NG_MANAGER;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
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
@StoreIn(NG_MANAGER)
public class Migration {
  @Id @org.springframework.data.annotation.Id String id;
  private Date startedAt;
  private Date endedAt;
  @FdIndex private String accountId;
  List<RoleAssignmentResponseDTO> createdRoleAssignments;
  String exception;
}
