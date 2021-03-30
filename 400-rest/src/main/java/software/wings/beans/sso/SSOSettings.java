package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@OwnedBy(PL)
@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "SSOSettingsKeys")
@Entity(value = "ssoSettings")
@HarnessEntity(exportable = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SamlSettings.class, name = "SAML")
      , @JsonSubTypes.Type(value = OauthSettings.class, name = "OAUTH"),
          @JsonSubTypes.Type(value = LdapSettings.class, name = "LDAP")
    })
public abstract class SSOSettings extends Base implements AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountIdTypeIdx")
                 .field(SSOSettingsKeys.accountId)
                 .field(SSOSettingsKeys.type)
                 .build())
        .build();
  }

  @NotNull protected SSOType type;
  @NotEmpty protected String displayName;
  @NotEmpty protected String url;

  public SSOSettings(SSOType type, String displayName, String url) {
    this.type = type;
    this.displayName = displayName;
    this.url = url;
    appId = GLOBAL_APP_ID;
  }

  // TODO: Return list of all sso settings instead with the use of @JsonIgnore to trim the unnecessary elements
  @JsonIgnore public abstract SSOSettings getPublicSSOSettings();

  public static final class SSOSettingsKeys { public static final String accountId = AccountAccess.ACCOUNT_ID_KEY; }
}
