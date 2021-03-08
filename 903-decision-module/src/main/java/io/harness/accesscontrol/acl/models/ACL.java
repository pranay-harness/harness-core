package io.harness.accesscontrol.acl.models;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.accesscontrol.acl.models.HACL.HACLKeys;
import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
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
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("acl")
@Entity(value = "acl", noClassnameStored = true)
@TypeAlias("acl")
@StoreIn(ACCESS_CONTROL)
public abstract class ACL implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  @CreatedDate Long createdAt;
  @Version Long version;
  @LastModifiedDate Long lastModifiedAt;
  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastModifiedBy;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("aclQueryStringUniqueIdx")
                 .unique(true)
                 .field(HACLKeys.aclQueryString)
                 .build())
        .build();
  }
}
