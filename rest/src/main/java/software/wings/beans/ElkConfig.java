package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.settings.SettingValue;
import software.wings.stencils.DefaultValue;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * The type ELK config.
 */
@JsonTypeName("ELK")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
public class ElkConfig extends SettingValue implements Encryptable {
  @Attributes(required = true, title = "Connector type")
  @DefaultValue("ELASTIC_SEARCH_SERVER")
  private ElkConnector elkConnector;

  @Attributes(title = "URL", required = true) @NotEmpty private String elkUrl;

  @Attributes(title = "Username") private String username;

  @JsonView(JsonViews.Internal.class) @Attributes(title = "Password") @Encrypted private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  private String kibanaVersion = "0";

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Elk config.
   */
  public ElkConfig() {
    super(SettingVariableTypes.ELK.name());
  }

  public ElkConfig(SettingVariableTypes type) {
    super(type.name());
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String elkUrl;
    private String username;
    private String password;
    private String connectorType;

    @Builder
    public Yaml(String type, String elkUrl, String username, String password, String connectorType) {
      super(type);
      this.elkUrl = elkUrl;
      this.username = username;
      this.password = password;
      this.connectorType = connectorType;
    }
  }
}
