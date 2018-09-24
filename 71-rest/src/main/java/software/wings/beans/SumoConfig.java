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
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
@JsonTypeName("SUMO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"accessId", "accessKey"})
@Builder
public class SumoConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Sumo Logic API Server URL", required = true) @NotEmpty private String sumoUrl;

  @Attributes(title = "Access ID", required = true) @Encrypted private char[] accessId;

  @Attributes(title = "Access Key", required = true) @Encrypted private char[] accessKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedAccessId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedAccessKey;
  /**
   * Instantiates a new setting value.
   **/
  public SumoConfig() {
    super(SettingVariableTypes.SUMO.name());
  }

  private SumoConfig(String sumoUrl, char[] accessId, char[] accessKey, String accountId, String encryptedAccessId,
      String encryptedAccessKey) {
    this();
    this.sumoUrl = sumoUrl;
    this.accessId = accessId.clone();
    this.accessKey = accessKey.clone();
    this.accountId = accountId;
    this.encryptedAccessId = encryptedAccessId;
    this.encryptedAccessKey = encryptedAccessKey;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String sumoUrl;
    private String accessId;
    private String accessKey;

    @Builder
    public Yaml(String type, String harnessApiVersion, String sumoUrl, String accessId, String accessKey,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.sumoUrl = sumoUrl;
      this.accessId = accessId;
      this.accessKey = accessKey;
    }
  }
}
