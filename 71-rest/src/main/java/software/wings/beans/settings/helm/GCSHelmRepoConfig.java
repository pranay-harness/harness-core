package software.wings.beans.settings.helm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.HelmRepoYaml;

import javax.validation.constraints.NotNull;

@JsonTypeName("GCS_HELM_REPO")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GCSHelmRepoConfig extends SettingValue implements HelmRepoConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String connectorId;
  @NotEmpty private String bucketName;
  @NotNull private String folderPath;

  public GCSHelmRepoConfig() {
    super(SettingVariableTypes.GCS_HELM_REPO.name());
  }

  public GCSHelmRepoConfig(String accountId, String connectorId, String bucketName, String folderPath) {
    super(SettingVariableTypes.GCS_HELM_REPO.name());
    this.accountId = accountId;
    this.connectorId = connectorId;
    this.bucketName = bucketName;
    this.folderPath = folderPath;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends HelmRepoYaml {
    private String cloudProvider;
    private String bucket;
    private String folderPath;

    @Builder
    public Yaml(String type, String harnessApiVersion, String cloudProvider, String bucket, String folderPath,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.cloudProvider = cloudProvider;
      this.bucket = bucket;
      this.folderPath = folderPath;
    }
  }
}
