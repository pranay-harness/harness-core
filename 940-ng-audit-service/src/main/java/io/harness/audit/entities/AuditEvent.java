package io.harness.audit.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.beans.AuthenticationInfo.AuthenticationInfoKeys;
import io.harness.audit.beans.Principal.PrincipalKeys;
import io.harness.audit.beans.YamlDiff;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.Resource;
import io.harness.ng.core.Resource.ResourceKeys;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.common.beans.KeyValuePair.KeyValuePairKeys;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;
import io.harness.scope.ResourceScope;
import io.harness.scope.ResourceScope.ResourceScopeKeys;

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

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "AuditEventKeys")
@Entity(value = "auditEvents", noClassnameStored = true)
@Document("auditEvents")
@TypeAlias("auditEvents")
@StoreIn(DbAliases.AUDITS)
public class AuditEvent {
  @NotBlank String insertId;
  @Id @org.mongodb.morphia.annotations.Id String id;

  @Valid @NotNull ResourceScope resourceScope;

  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;

  @NotNull Long timestamp;

  @NotNull @Valid AuthenticationInfo authenticationInfo;

  @NotNull ModuleType module;

  @NotNull @Valid Resource resource;
  @NotBlank String action;

  YamlDiff yamlDiff;
  @Valid AuditEventData auditEventData;

  List<KeyValuePair> additionalInfo;

  @CreatedDate Long createdAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeScopeModulePrincipalResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.ORG_IDENTIFIER_KEY)
                 .field(AuditEventKeys.PROJECT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.module)
                 .field(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                 .field(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeScopeModuleResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.ORG_IDENTIFIER_KEY)
                 .field(AuditEventKeys.PROJECT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.module)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeScopePrincipalResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.ORG_IDENTIFIER_KEY)
                 .field(AuditEventKeys.PROJECT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                 .field(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeScopeResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.ORG_IDENTIFIER_KEY)
                 .field(AuditEventKeys.PROJECT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_LABEL_VALUES_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("uniqueNgAuditEventIdx")
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.insertId)
                 .field(AuditEventKeys.timestamp)
                 .unique(true)
                 .build())
        .build();
  }

  @UtilityClass
  public static final class AuditEventKeys {
    public static final String ACCOUNT_IDENTIFIER_KEY =
        AuditEventKeys.resourceScope + "." + ResourceScopeKeys.accountIdentifier;
    public static final String ORG_IDENTIFIER_KEY =
        AuditEventKeys.resourceScope + "." + ResourceScopeKeys.orgIdentifier;
    public static final String PROJECT_IDENTIFIER_KEY =
        AuditEventKeys.resourceScope + "." + ResourceScopeKeys.projectIdentifier;

    public static final String PRINCIPAL_TYPE_KEY =
        AuditEventKeys.authenticationInfo + "." + AuthenticationInfoKeys.principal + "." + PrincipalKeys.type;
    public static final String PRINCIPAL_IDENTIFIER_KEY =
        AuditEventKeys.authenticationInfo + "." + AuthenticationInfoKeys.principal + "." + PrincipalKeys.identifier;

    public static final String RESOURCE_TYPE_KEY = AuditEventKeys.resource + "." + ResourceKeys.type;
    public static final String RESOURCE_IDENTIFIER_KEY = AuditEventKeys.resource + "." + ResourceKeys.identifier;

    public static final String RESOURCE_LABEL_KEY = AuditEventKeys.resource + "." + ResourceKeys.labels;
    public static final String RESOURCE_LABEL_KEYS_KEY =
        AuditEventKeys.resource + "." + ResourceKeys.labels + "." + KeyValuePairKeys.key;
    public static final String RESOURCE_LABEL_VALUES_KEY =
        AuditEventKeys.resource + "." + ResourceKeys.labels + "." + KeyValuePairKeys.value;
  }
}
