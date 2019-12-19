package software.wings.beans.settings.helm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.audit.ResourceType;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.HelmRepoYaml;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("AMAZON_S3_HELM_REPO")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AmazonS3HelmRepoConfig extends SettingValue implements HelmRepoConfig {
  private static final String AWS_URL = "https://aws.amazon.com/";
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String connectorId;
  @NotEmpty private String bucketName;
  private String folderPath;
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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilityList = new ArrayList<>();
    executionCapabilityList.addAll(CapabilityHelper.generateExecutionCapabilitiesForHelm(new ArrayList<>()));
    executionCapabilityList.addAll(CapabilityHelper.generateExecutionCapabilitiesForChartMuseum(new ArrayList<>()));
    executionCapabilityList.add(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL));
    return executionCapabilityList;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends HelmRepoYaml {
    private String cloudProvider;
    private String bucket;
    private String region;

    @Builder
    public Yaml(String type, String harnessApiVersion, String cloudProvider, String bucket, String region,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.cloudProvider = cloudProvider;
      this.bucket = bucket;
      this.region = region;
    }
  }
}
