package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.secretmanagerclient.SecretType;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Optional;

@Data
@Builder
@FieldNameConstants(innerTypeName = "SecretKeys")
@Entity(value = "secrets", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("secrets")
@CdUniqueIndex(name = "unique_accountIdentifier_organizationIdentifier_projectIdentifier_identifier",
    fields =
    {
      @Field(SecretKeys.accountIdentifier)
      , @Field(SecretKeys.orgIdentifier), @Field(SecretKeys.projectIdentifier), @Field(SecretKeys.identifier)
    })
public class Secret {
  @Id @org.mongodb.morphia.annotations.Id String id;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  String description;
  Map<String, String> tags;
  SecretType type;
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
        .tags(getTags())
        .type(getType())
        .spec(Optional.ofNullable(getSecretSpec()).map(SecretSpec::toDTO).orElse(null))
        .build();
  }
}
