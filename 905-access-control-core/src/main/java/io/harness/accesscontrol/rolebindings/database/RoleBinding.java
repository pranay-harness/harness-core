package io.harness.accesscontrol.rolebindings.database;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@FieldNameConstants(innerTypeName = "RoleBindingKeys")
@Document("rolebindings")
@TypeAlias("rolebindings")
public class RoleBinding implements PersistentEntity {
  @Id String id;
  @EntityIdentifier String identifier;
  @NotEmpty String parentIdentifier;
  @NotEmpty String resourceGroupIdentifier;
  @NotEmpty String roleIdentifier;
  @NotEmpty String principalIdentifier;
  @NotNull PrincipalType principalType;
  boolean isDefault;
  boolean isDisabled;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @Version Long version;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("uniqueIndex")
                 .unique(true)
                 .field(RoleBindingKeys.identifier)
                 .field(RoleBindingKeys.parentIdentifier)
                 .build())
        .build();
  }
}
