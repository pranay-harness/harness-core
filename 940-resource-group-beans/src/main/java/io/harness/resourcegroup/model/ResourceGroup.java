package io.harness.resourcegroup.model;

import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
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

@Data
@Builder
@FieldNameConstants(innerTypeName = "ResourceGroupKeys")
@Document("ResourceGroup")
@TypeAlias("ResourceGroup")
public class ResourceGroup implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ResourceGroupPrimaryKey")
                 .field(ResourceGroupKeys.accountIdentifier)
                 .field(ResourceGroupKeys.orgIdentifier)
                 .field(ResourceGroupKeys.projectIdentifier)
                 .field(ResourceGroupKeys.identifier)
                 .unique(true)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotEmpty String identifier;
  @NotEmpty String name;
  String description;
  @NotNull @Builder.Default Boolean system;
  @NotEmpty List<ResourceSelector> resourceSelectors;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @Version Long version;
}
