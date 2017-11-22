package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
@JsonTypeName("SUMO")
@Data
@ToString(exclude = {"accessId", "accessKey"})
public class SumoConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Sumo Logic API Server URL", required = true) @NotEmpty private String sumoUrl;

  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Access ID", required = true)
  @NotEmpty
  @Encrypted
  private char[] accessId;

  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Access Key", required = true)
  @NotEmpty
  @Encrypted
  private char[] accessKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedAccessId;

  @SchemaIgnore private String encryptedAccessKey;
  /**
   * Instantiates a new setting value.
   **/
  public SumoConfig() {
    super(SettingVariableTypes.SUMO.name());
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String sumoUrl;
    private String accessId;
    private String accessKey;

    public Yaml() {}

    public Yaml(String type, String name, String sumoUrl, String accessId, String accessKey) {
      super(type, name);
      this.sumoUrl = sumoUrl;
      this.accessId = accessId;
      this.accessKey = accessKey;
    }
  }
}
