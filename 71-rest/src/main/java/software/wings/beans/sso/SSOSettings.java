package software.wings.beans.sso;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "SSOSettingsKeys")
@Entity(value = "ssoSettings")
@HarnessEntity(exportable = true)
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
