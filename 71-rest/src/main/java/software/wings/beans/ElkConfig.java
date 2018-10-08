package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.impl.analysis.ElkValidationTypeProvider;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * The type ELK config.
 */
@JsonTypeName("ELK")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
@Builder
public class ElkConfig extends SettingValue implements EncryptableSetting {
  @Attributes(required = true, title = "Connector type")
  @DefaultValue("ELASTIC_SEARCH_SERVER")
  private ElkConnector elkConnector;

  @Attributes(title = "URL", required = true) @NotEmpty private String elkUrl;

  @Attributes(title = "Username") private String username;

  @Attributes(title = "Password") @Encrypted private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  private String kibanaVersion = "0";

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  @Attributes(required = true, title = "Authentication")
  @DefaultValue("Password")
  private ElkValidationType validationType;

  @Attributes(required = true, title = "Authentication")
  @DefaultValue("Password")
  @EnumData(enumDataProvider = ElkValidationTypeProvider.class)
  public ElkValidationType getValidationType() {
    if (validationType == null) {
      return ElkValidationType.PASSWORD;
    }

    return validationType;
  }

  public void setElkValidationType(ElkValidationType validationType) {
    this.validationType = validationType;
  }
  /**
   * Instantiates a new Elk config.
   */
  public ElkConfig() {
    super(SettingVariableTypes.ELK.name());
  }

  private ElkConfig(ElkConnector elkConnector, String elkUrl, String username, char[] password, String accountId,
      String kibanaVersion, String encryptedPassword, ElkValidationType validationType) {
    this();
    this.elkConnector = elkConnector;
    this.elkUrl = elkUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.kibanaVersion = kibanaVersion;
    this.encryptedPassword = encryptedPassword;
    this.validationType = validationType;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String elkUrl;
    private String username;
    private String password;
    private String connectorType;
    private ElkValidationType validationType;

    @Builder
    public Yaml(String type, String harnessApiVersion, String elkUrl, ElkValidationType validationType, String username,
        String password, String connectorType, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.elkUrl = elkUrl;
      this.validationType = validationType;
      this.username = username;
      this.password = password;
      this.connectorType = connectorType;
    }
  }
}
