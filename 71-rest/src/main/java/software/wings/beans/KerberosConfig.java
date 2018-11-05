package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.beans.Transient;

/**
 * Created by Aaditi Joag on 10/16/18.
 */
@JsonTypeName("KERBEROS_CONFIG")
@Data
public class KerberosConfig {
  @Attributes(title = "Principal", required = true) @NotEmpty private String principal;
  @Attributes(title = "Generate TGT") private boolean generateTGT;
  @Attributes(title = "Realm", required = true) @NotEmpty private String realm;
  @Attributes(title = "KeyTab File Path") private String keyTabFilePath;

  @JsonIgnore
  @Transient
  public String getPrincipalWithRealm() {
    return realm != null && realm.length() != 0 ? principal + "@" + realm : principal;
  }
}