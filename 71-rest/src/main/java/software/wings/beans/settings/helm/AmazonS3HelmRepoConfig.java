package software.wings.beans.settings.helm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.settings.SettingValue;

@JsonTypeName("AMAZON_S3_HELM_REPO")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AmazonS3HelmRepoConfig extends SettingValue implements HelmRepoConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String connectorId;
  @NotEmpty private String bucketName;
  @NotEmpty private String folderPath;
  @NotEmpty private String region;

  public AmazonS3HelmRepoConfig() {
    super(SettingVariableTypes.AMAZON_S3_HELM_REPO.name());
  }

  public AmazonS3HelmRepoConfig(
      String accountId, String connectorId, String bucketName, String folderPath, String region) {
    super(SettingVariableTypes.AMAZON_S3_HELM_REPO.name());
    this.accountId = accountId;
    this.connectorId = connectorId;
    this.bucketName = bucketName;
    this.folderPath = folderPath;
    this.region = region;
  }
}
