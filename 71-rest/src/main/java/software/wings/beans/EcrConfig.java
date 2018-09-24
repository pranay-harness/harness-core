package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * ECR Artifact Server / Connector has been deprecated.
 * Instead, we use AWS cloud provider to fetch all the connection details.
 * This class is not deleted since there might be existing configs in the mongo db.
 * We can only delete this class when the entries are migrated to use cloud provider.
 * Created by brett on 7/16/17
 */
@JsonTypeName("ECR")
@Deprecated
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "secretKey")
public class EcrConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Amazon ECR Registry URL", required = true) @NotEmpty private String ecrUrl;
  @Attributes(title = "Access Key", required = true) @NotEmpty private String accessKey;
  @Attributes(title = "Secret Key", required = true) @Encrypted private char[] secretKey;
  @Attributes(title = "Region", required = true)
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSecretKey;

  /**
   * Instantiates a new ECR registry config.
   */
  public EcrConfig() {
    super(SettingVariableTypes.ECR.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EcrConfig(
      String ecrUrl, String accessKey, char[] secretKey, String region, String accountId, String encryptedSecretKey) {
    this();
    this.ecrUrl = ecrUrl;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
  }
}
