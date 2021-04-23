package io.harness.connector.entities;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.connector.ConnectorActivityDetails;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorConnectivityDetails.ConnectorConnectivityDetailsKeys;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "ConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn(DbAliases.NG_MANAGER)
@Document("connectors")
@Persistent
@OwnedBy(HarnessTeam.DX)
public abstract class Connector extends GitSyncableEntity implements PersistentEntity, NGAccountAccess {
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;
  @NotEmpty io.harness.encryption.Scope scope;
  String description;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed String orgIdentifier;
  @Trimmed String projectIdentifier;
  @NotEmpty String fullyQualifiedIdentifier;
  @NotEmpty ConnectorType type;
  @NotEmpty List<ConnectorCategory> categories;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  Set<String> delegateSelectors;
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
                 .name("accountId_orgId_projectId_name_Index")
                 .fields(Arrays.asList(ConnectorKeys.accountIdentifier, ConnectorKeys.orgIdentifier,
                     ConnectorKeys.projectIdentifier, ConnectorKeys.name))
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("fullyQualifiedIdentifier_deleted_Index")
                 .fields(Arrays.asList(ConnectorKeys.fullyQualifiedIdentifier, ConnectorKeys.deleted))
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_type_status_deletedAt_decreasing_sort_Index")
                 .fields(Arrays.asList(ConnectorKeys.accountIdentifier, ConnectorKeys.orgIdentifier,
                     ConnectorKeys.projectIdentifier, ConnectorKeys.type, ConnectorKeys.connectionStatus,
                     ConnectorKeys.deleted))
                 .descSortField(ConnectorKeys.createdAt)
                 .build())
        .build();
  }
}