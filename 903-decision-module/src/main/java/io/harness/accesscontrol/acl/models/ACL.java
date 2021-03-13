package io.harness.accesscontrol.acl.models;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.accesscontrol.acl.models.SourceMetadata.SourceMetadataKeys;
import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ACLKeys")
@Document("acl")
@Entity(value = "acl", noClassnameStored = true)
@TypeAlias("acl")
@StoreIn(ACCESS_CONTROL)
public class ACL implements PersistentEntity {
  private static final String DELIMITER = "$";

  @Id @org.mongodb.morphia.annotations.Id private String id;
  @CreatedDate Long createdAt;
  @Version Long version;
  @LastModifiedDate Long lastModifiedAt;
  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastModifiedBy;

  @FdIndex String roleAssignmentId;
  String scopeIdentifier;
  String permissionIdentifier;
  SourceMetadata sourceMetadata;
  String resource;
  String principalType;
  String principalIdentifier;
  @FdIndex String aclQueryString;

  public static String getAclQueryString(String scopeIdentifier, String resource, String principalType,
      String principalIdentifier, String permissionIdentifier) {
    return scopeIdentifier + DELIMITER + permissionIdentifier + DELIMITER + resource + DELIMITER + principalType
        + DELIMITER + principalIdentifier;
  }

  public static String getAclQueryString(@NotNull ACL acl) {
    return getAclQueryString(acl.getScopeIdentifier(), acl.getResource(), acl.getPrincipalType(),
        acl.getPrincipalIdentifier(), acl.getPermissionIdentifier());
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("uniqueIdx")
                 .field(ACLKeys.scopeIdentifier)
                 .field(SourceMetadataKeys.roleAssignmentIdentifier)
                 .field(SourceMetadataKeys.userGroupIdentifier)
                 .field(SourceMetadataKeys.roleIdentifier)
                 .field(SourceMetadataKeys.resourceGroupIdentifier)
                 .field(ACLKeys.permissionIdentifier)
                 .field(ACLKeys.principalIdentifier)
                 .field(ACLKeys.principalType)
                 .unique(true)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("roleIdx")
                 .field(ACLKeys.scopeIdentifier)
                 .field(SourceMetadataKeys.roleIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("resourceGroupIdx")
                 .field(ACLKeys.scopeIdentifier)
                 .field(SourceMetadataKeys.resourceGroupIdentifier)
                 .build())
        .build();
  }
}
