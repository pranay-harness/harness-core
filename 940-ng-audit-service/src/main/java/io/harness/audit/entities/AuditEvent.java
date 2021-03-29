package io.harness.audit.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventData;
import io.harness.audit.beans.AuthenticationInfoDBO;
import io.harness.audit.beans.AuthenticationInfoDBO.AuthenticationInfoKeys;
import io.harness.audit.beans.Principal.PrincipalKeys;
import io.harness.audit.beans.ResourceDBO;
import io.harness.audit.beans.ResourceDBO.ResourceKeys;
import io.harness.audit.beans.ResourceScopeDBO;
import io.harness.audit.beans.ResourceScopeDBO.ResourceScopeKeys;
import io.harness.audit.beans.YamlDiff;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.common.beans.KeyValuePair.KeyValuePairKeys;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(NON_NULL)
@StoreIn(DbAliases.AUDITS)
public class AuditEvent {
  @NotBlank String insertId;
  @Id @org.mongodb.morphia.annotations.Id String id;

  @Valid @NotNull ResourceScopeDBO resourceScope;

  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;

  @NotNull Long timestamp;

  @NotNull @Valid AuthenticationInfoDBO authenticationInfo;

  @NotNull @Valid ResourceDBO resource;

  YamlDiff yamlDiff;
  @Valid AuditEventData auditEventData;

  List<KeyValuePair> coreInfo;
  List<KeyValuePair> additionalInfo;

  @CreatedDate Long createdAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountCorePrincipalResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.CORE_INFO_KEYS_KEY)
                 .field(AuditEventKeys.CORE_INFO_VALUES_KEY)
                 .field(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                 .field(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountCoreResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.CORE_INFO_KEYS_KEY)
                 .field(AuditEventKeys.CORE_INFO_VALUES_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountPrincipalResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                 .field(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountScopeCorePrincipalResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_VALUES_KEY)
                 .field(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                 .field(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountScopeCoreResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_VALUES_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountScopePrincipalResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_VALUES_KEY)
                 .field(AuditEventKeys.PRINCIPAL_TYPE_KEY)
                 .field(AuditEventKeys.PRINCIPAL_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ngAuditTimeAccountScopeResourceIdx")
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_KEYS_KEY)
                 .field(AuditEventKeys.RESOURCE_SCOPE_LABEL_VALUES_KEY)
                 .field(AuditEventKeys.RESOURCE_TYPE_KEY)
                 .field(AuditEventKeys.RESOURCE_IDENTIFIER_KEY)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("uniqueNgAuditEventIdx")
                 .field(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                 .field(AuditEventKeys.timestamp)
                 .field(AuditEventKeys.insertId)
                 .unique(true)
                 .build())
        .build();
  }

  @UtilityClass
  public static final class AuditEventKeys {
    public static final String ACCOUNT_IDENTIFIER_KEY =
        AuditEventKeys.resourceScope + "." + ResourceScopeKeys.accountIdentifier;
    public static final String RESOURCE_SCOPE_LABEL_KEY = AuditEventKeys.resourceScope + "." + ResourceScopeKeys.labels;
    public static final String RESOURCE_SCOPE_LABEL_KEYS_KEY =
        AuditEventKeys.resourceScope + "." + ResourceScopeKeys.labels + "." + KeyValuePairKeys.key;
    public static final String RESOURCE_SCOPE_LABEL_VALUES_KEY =
        AuditEventKeys.resourceScope + "." + ResourceScopeKeys.labels + "." + KeyValuePairKeys.value;

    public static final String PRINCIPAL_TYPE_KEY =
        AuditEventKeys.authenticationInfo + "." + AuthenticationInfoKeys.principal + "." + PrincipalKeys.type;
    public static final String PRINCIPAL_IDENTIFIER_KEY =
        AuditEventKeys.authenticationInfo + "." + AuthenticationInfoKeys.principal + "." + PrincipalKeys.identifier;

    public static final String RESOURCE_TYPE_KEY = AuditEventKeys.resource + "." + ResourceKeys.type;
    public static final String RESOURCE_IDENTIFIER_KEY = AuditEventKeys.resource + "." + ResourceKeys.identifier;

    public static final String CORE_INFO_KEYS_KEY = AuditEventKeys.coreInfo + "." + KeyValuePairKeys.key;
    public static final String CORE_INFO_VALUES_KEY = AuditEventKeys.coreInfo + "." + KeyValuePairKeys.value;
  }
}
