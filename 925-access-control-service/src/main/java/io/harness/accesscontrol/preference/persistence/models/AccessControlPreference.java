package io.harness.accesscontrol.preference.persistence.models;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "AccessControlPreferenceKeys")
@Document("aclPreferences")
@Entity(value = "aclPreferences", noClassnameStored = true)
@TypeAlias("aclPreferences")
@StoreIn(ACCESS_CONTROL)
public class AccessControlPreference {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @FdUniqueIndex String accountId;
  boolean accessControlEnabled;
}
