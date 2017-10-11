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
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

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
}
