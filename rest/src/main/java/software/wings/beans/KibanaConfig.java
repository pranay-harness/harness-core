package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;

/**
 * Created by sriram_parthasarathy on 10/2/17.
 */
@JsonTypeName("KIBANA")
@Data
@ToString(exclude = "password")
public class KibanaConfig extends ElkConfig {
  @Attributes(title = "Kibana URL", required = true)
  @NotEmpty
  public String getElkUrl() {
    return super.getElkUrl();
  }

  @SchemaIgnore
  public String getAccountId() {
    return super.getAccountId();
  }

  @Attributes(title = "Username")
  public String getUsername() {
    return super.getUsername();
  }

  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password")
  public char[] getPassword() {
    return super.getPassword();
  }

  @Attributes(title = "Kibana Version", required = true) @NotEmpty private String kibanaVersion;

  /**
   * Instantiates a new Splunk config.
   */
  public KibanaConfig() {
    super(SettingVariableTypes.KIBANA);
  }
}
