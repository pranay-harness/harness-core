package io.harness.audit.entities;

import io.harness.ModuleType;
import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.beans.AuthenticationInfo.AuthenticationInfoKeys;
import io.harness.audit.beans.YamlDiff;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.Resource;
import io.harness.ng.core.Resource.ResourceKeys;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.common.beans.KeyValuePair.KeyValuePairKeys;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;
import io.harness.security.dto.Principal.PrincipalKeys;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AuditEventKeys")
@Entity(value = "auditEvents", noClassnameStored = true)
@Document("auditEvents")
@TypeAlias("auditEvents")
public class AuditEvent {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotBlank String insertId;
  @NotBlank String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;

  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;

  @NotNull Long timestamp;

  @NotNull @Valid AuthenticationInfo authenticationInfo;

  @NotNull ModuleType moduleType;

  @NotNull @Valid Resource resource;
  @NotBlank String action;

  YamlDiff yamlDiff;
  @Valid AuditEventData auditEventData;

  List<KeyValuePair> additionalInfo;

  @CreatedDate Long createdAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditEventScopeIdx")
                 .field(AuditEventKeys.accountIdentifier)
                 .field(AuditEventKeys.orgIdentifier)
                 .field(AuditEventKeys.projectIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditEventPrincipalIdx")
                 .field(AuditEventKeys.PRINCIPAL_TYPE)
                 .field(AuditEventKeys.PRINCIPAL_NAME)
                 .build())
        .add(CompoundMongoIndex.builder().name("ngAuditEventModuleTypeIdx").field(AuditEventKeys.moduleType).build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditEventResourceIdx")
                 .field(AuditEventKeys.RESOURCE_TYPE)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER)
                 .field(AuditEventKeys.RESOURCE_LABEL_KEYS)
                 .field(AuditEventKeys.RESOURCE_LABEL_VALUES)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditEventUniqueIdx")
                 .field(AuditEventKeys.accountIdentifier)
                 .field(AuditEventKeys.insertId)
                 .field(AuditEventKeys.timestamp)
                 .unique(true)
                 .build())
        .build();
  }

  @UtilityClass
  public static final class AuditEventKeys {
    public static final String PRINCIPAL_TYPE =
        AuditEventKeys.authenticationInfo + "." + AuthenticationInfoKeys.principal + "." + PrincipalKeys.type;
    public static final String PRINCIPAL_NAME =
        AuditEventKeys.authenticationInfo + "." + AuthenticationInfoKeys.principal + "." + PrincipalKeys.name;

    public static final String RESOURCE_TYPE = AuditEventKeys.resource + "." + ResourceKeys.type;
    public static final String RESOURCE_IDENTIFIER = AuditEventKeys.resource + "." + ResourceKeys.identifier;

    public static final String RESOURCE_LABEL_KEYS =
        AuditEventKeys.resource + "." + ResourceKeys.labels + "." + KeyValuePairKeys.key;
    public static final String RESOURCE_LABEL_VALUES =
        AuditEventKeys.resource + "." + ResourceKeys.labels + "." + KeyValuePairKeys.value;
  }
}
