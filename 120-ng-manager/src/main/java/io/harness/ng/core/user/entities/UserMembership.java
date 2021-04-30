package io.harness.ng.core.user.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "UserMembershipKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "userMemberships", noClassnameStored = true)
@Document("userMemberships")
@TypeAlias("userMemberships")
@StoreIn(DbAliases.NG_MANAGER)
@OwnedBy(PL)
public class UserMembership implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("userMembershipUserIdScopes")
                 .field(UserMembershipKeys.userId)
                 .field(UserMembershipKeys.scopes)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("userMembershipAccountOrgProject")
                 .field(UserMembershipKeys.scopes + "."
                     + "accountIdentifier")
                 .field(UserMembershipKeys.scopes + "."
                     + "orgIdentifier")
                 .field(UserMembershipKeys.scopes + "."
                     + "projectIdentifier")
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("uniqueUserMembershipUserId")
                 .field(UserMembershipKeys.userId)
                 .unique(true)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotEmpty String userId;
  @NotEmpty String emailId;
  @Valid List<Scope> scopes;
  @Version Long version;
}
