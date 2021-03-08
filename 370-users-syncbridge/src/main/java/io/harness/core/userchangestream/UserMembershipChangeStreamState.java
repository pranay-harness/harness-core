package io.harness.core.userchangestream;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(PL)
@Value
@Builder
@Entity(value = "userEntityChangeStreamState", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "UserEntityChangeStreamStateKeys")
@Slf4j
public class UserMembershipChangeStreamState implements PersistentEntity {
  public static final String ID_VALUE = "lastSyncedToken";
  @Id @Builder.Default String id = ID_VALUE;
  String lastSyncedToken;
}
