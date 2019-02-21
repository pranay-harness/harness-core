package software.wings.beans.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import javax.validation.constraints.NotNull;

@Entity(value = "ssoSettings")
@HarnessExportableEntity
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class SSOSettings extends Base {
  @NotNull @NaturalKey protected SSOType type;
  @NotEmpty protected String displayName;
  @NotEmpty @NaturalKey protected String url;

  public SSOSettings(SSOType type, String displayName, String url) {
    this.type = type;
    this.displayName = displayName;
    this.url = url;
    appId = GLOBAL_APP_ID;
  }

  // TODO: Return list of all sso settings instead with the use of @JsonIgnore to trim the unnecessary elements
  @JsonIgnore public abstract SSOSettings getPublicSSOSettings();
}
