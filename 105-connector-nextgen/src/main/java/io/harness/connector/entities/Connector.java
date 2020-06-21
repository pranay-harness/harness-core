package io.harness.connector.entities;

import io.harness.connector.common.ConnectorCategory;
import io.harness.connector.common.ConnectorType;
import io.harness.connector.entities.connectivityStatus.ConnectivityStatus;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@FieldNameConstants(innerTypeName = "ConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Document("connectors")
@TypeAlias("connectors")
// todo deepak: Add index after adding the queries
public abstract class Connector {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;

  // todo deepak: Where we should keep the scope, it will be used by everyone
  @NotEmpty Scope scope;
  @Trimmed @NotEmpty String accountId;
  @Trimmed @NotEmpty String orgId;
  @Trimmed @NotEmpty String projectId;
  @NotEmpty ConnectorType type;
  @NotEmpty List<ConnectorCategory> categories;
  @NotEmpty ConnectivityStatus status;

  @NotNull @Singular @Size(max = 128) List<String> tags;
  // todo deepak: Add createdBy once user entity is decided
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  public enum Scope { ACCOUNT, PROJECT, ORGANIZATION }
}
