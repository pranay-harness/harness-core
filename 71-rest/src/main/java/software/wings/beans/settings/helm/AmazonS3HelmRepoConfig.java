package software.wings.beans.settings.helm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.audit.ResourceType;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.HelmRepoYaml;

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

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends HelmRepoYaml {
    private String cloudProvider;
    private String bucket;
    private String folderPath;
    private String region;

    @Builder
    public Yaml(String type, String harnessApiVersion, String cloudProvider, String bucket, String folderPath,
        String region, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.cloudProvider = cloudProvider;
      this.bucket = bucket;
      this.folderPath = folderPath;
      this.region = region;
    }
  }
}
