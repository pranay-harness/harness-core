package software.wings.beans.settings.helm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.audit.ResourceType;
import software.wings.delegatetasks.validation.capabilities.HelmInstallationCapability;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.HelmRepoYaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonTypeName("HTTP_HELM_REPO")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class HttpHelmRepoConfig extends SettingValue implements HelmRepoConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String chartRepoUrl;
  private String username;
  @Encrypted(fieldName = "password") private char[] password;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public HttpHelmRepoConfig() {
    super(SettingVariableTypes.HTTP_HELM_REPO.name());
  }

  public HttpHelmRepoConfig(
      String accountId, String chartRepoUrl, String username, final char[] password, String encryptedPassword) {
    super(SettingVariableTypes.HTTP_HELM_REPO.name());
    this.accountId = accountId;
    this.chartRepoUrl = chartRepoUrl;
    this.username = username;
    this.password = (password != null) ? Arrays.copyOf(password, password.length) : null;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public String getConnectorId() {
    return null;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilityList = new ArrayList<>();
    executionCapabilityList.add(HelmInstallationCapability.builder()
                                    .version(HelmConstants.HelmVersion.V3)
                                    .criteria("HTTP_HELM_REPO: " + getChartRepoUrl())
                                    .build());
    executionCapabilityList.add(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(chartRepoUrl));
    return executionCapabilityList;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends HelmRepoYaml {
    private String url;
    private String username;
    private String password;

    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.url = url;
      this.username = username;
      this.password = password;
    }
  }
}
