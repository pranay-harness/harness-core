package io.harness.ng.core.models;

import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.annotation.StoreIn;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.SecretType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "SecretKeys")
@Entity(value = "secrets", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("secrets")
@StoreIn(DbAliases.NG_MANAGER)
public class Secret implements PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_identification")
                 .unique(true)
                 .field(SecretKeys.accountIdentifier)
                 .field(SecretKeys.orgIdentifier)
                 .field(SecretKeys.projectIdentifier)
                 .field(SecretKeys.identifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("nextIterationWithMigrationIdx")
                 .field(SecretKeys.migratedFromManager)
                 .field(SecretKeys.nextIteration)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String id;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  String description;
  List<NGTag> tags;
  SecretType type;
  Boolean draft;

  public boolean isDraft() {
    return draft != null && draft;
  }

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
  SecretSpec secretSpec;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  public SecretDTOV2 toDTO() {
    return SecretDTOV2.builder()
        .orgIdentifier(getOrgIdentifier())
        .projectIdentifier(getProjectIdentifier())
        .identifier(getIdentifier())
        .name(getName())
        .description(getDescription())
        .tags(convertToMap(getTags()))
        .type(getType())
        .spec(Optional.ofNullable(getSecretSpec()).map(SecretSpec::toDTO).orElse(null))
        .build();
  }

  Boolean migratedFromManager;

  @FdIndex Long nextIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return this.nextIteration;
  }

  @JsonIgnore
  @Override
  public String getUuid() {
    return this.id;
  }
}
