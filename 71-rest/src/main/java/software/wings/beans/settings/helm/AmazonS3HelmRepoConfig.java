package software.wings.beans.settings.helm;

import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE;
import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_STRING;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.validator.EntityName;
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
  @NotEmpty
  @EntityName(charSetString = ALLOWED_CHARS_SERVICE_VARIABLE_STRING, message = ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE)
  private String repoName;
  @NotEmpty private String bucketName;
  @NotEmpty private String folderPath;
  @NotEmpty private String region;

  public AmazonS3HelmRepoConfig() {
    super(SettingVariableTypes.AMAZON_S3_HELM_REPO.name());
  }

  public AmazonS3HelmRepoConfig(
      String accountId, String connectorId, String repoName, String bucketName, String folderPath, String region) {
    super(SettingVariableTypes.AMAZON_S3_HELM_REPO.name());
    this.accountId = accountId;
    this.connectorId = connectorId;
    this.repoName = repoName;
    this.bucketName = bucketName;
    this.folderPath = folderPath;
    this.region = region;
  }
}
