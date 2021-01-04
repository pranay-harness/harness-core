package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.beans.sso.SSOSettings.SSOSettingsKeys;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@CdIndex(name = "accountIdTypeIdx", fields = { @Field("accountId")
                                               , @Field(SSOSettingsKeys.type) })
public abstract class SSOSettings extends Base implements AccountAccess {
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
}
