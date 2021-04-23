package io.harness.ng.core.user.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
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
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
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
                 .field(UserMembershipKeys.SCOPE_ACCOUNT_IDENTIFIER_KEY)
                 .field(UserMembershipKeys.SCOPE_ORG_IDENTIFIER_KEY)
                 .field(UserMembershipKeys.SCOPE_PROJECT_IDENTIFIER_KEY)
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

  @Value
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @FieldNameConstants(innerTypeName = "ScopeKeys")
  public static class Scope {
    @NotEmpty String accountIdentifier;
    String orgIdentifier;
    String projectIdentifier;
  }

  @UtilityClass
  public static final class UserMembershipKeys {
    public static final String SCOPE_ACCOUNT_IDENTIFIER_KEY = UserMembershipKeys.scopes + "."
        + "accountIdentifier";
    public static final String SCOPE_ORG_IDENTIFIER_KEY = UserMembershipKeys.scopes + "."
        + "orgIdentifier";
    public static final String SCOPE_PROJECT_IDENTIFIER_KEY = UserMembershipKeys.scopes + "."
        + "projectIdentifier";
  }
}
