package io.harness.cdng.core.entities;

import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.springframework.data.annotation.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ProjectKeys")
@Entity(value = "projects", noClassnameStored = true)
public class Project
    implements PersistentEntity, UuidAware, UuidAccess, AccountAccess, CreatedAtAccess, CreatedAtAware {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @Indexed String accountId;
  String name;
  long createdAt;
}
