package io.harness.connector.entities;

import io.harness.beans.EmbeddedUser;
import io.harness.connector.ConnectorActivityDetails;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorConnectivityDetails.ConnectorConnectivityDetailsKeys;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants(innerTypeName = "ConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("connectors")
public abstract class Connector implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;
  // todo deepak: Where we should keep the scope, it will be used by everyone
  @NotEmpty io.harness.encryption.Scope scope;
  String description;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  @NotEmpty String fullyQualifiedIdentifier;
  @NotEmpty ConnectorType type;
  @NotEmpty List<ConnectorCategory> categories;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  @CreatedBy private EmbeddedUser createdBy;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  Long timeWhenConnectorIsLastUpdated;
  ConnectorConnectivityDetails connectivityDetails;
  ConnectorActivityDetails activityDetails;
  Boolean deleted = Boolean.FALSE;
  String heartbeatPerpetualTaskId;

  @Override
  public String getAccountIdentifier() {
    return accountIdentifier;
  }

  public static final String CONNECTOR_COLLECTION_NAME = "connectors";

  @UtilityClass
  public static final class ConnectorKeys {
    public static final String connectionStatus =
        ConnectorKeys.connectivityDetails + "." + ConnectorConnectivityDetailsKeys.status;
    public static final String tagKey = ConnectorKeys.tags + "." + NGTagKeys.key;
    public static final String tagValue = ConnectorKeys.tags + "." + NGTagKeys.value;
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_fullyQualifiedIdentifier")
                 .unique(true)
                 .field(ConnectorKeys.fullyQualifiedIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_Index")
                 .fields(Arrays.asList(
                     ConnectorKeys.accountIdentifier, ConnectorKeys.orgIdentifier, ConnectorKeys.projectIdentifier))
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("fullyQualifiedIdentifier_deleted_Index")
                 .fields(Arrays.asList(ConnectorKeys.fullyQualifiedIdentifier, ConnectorKeys.deleted))
                 .build())
        .build();
  }
}
