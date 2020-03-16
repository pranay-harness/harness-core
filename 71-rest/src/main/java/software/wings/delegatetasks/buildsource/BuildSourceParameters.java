package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AZURE_ARTIFACTS;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.mixin.IgnoreValidationCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.settings.SettingValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class BuildSourceParameters implements TaskParameters, ExecutionCapabilityDemander {
  public enum BuildSourceRequestType { GET_BUILDS, GET_LAST_SUCCESSFUL_BUILD }

  @NotNull private BuildSourceRequestType buildSourceRequestType;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotNull private SettingValue settingValue;
  @NotNull private ArtifactStreamAttributes artifactStreamAttributes;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
  @NotEmpty private String artifactStreamType;
  private String artifactStreamId;
  private int limit;

  // These fields are used only during artifact collection and cleanup.
  private boolean isCollection;
  // Unique key representing build numbers already present in the DB. It stores different things for different artifact
  // stream types like buildNo, revision or artifactPath.
  private Set<String> savedBuildDetailsKeys;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    switch (settingValue.getSettingType()) {
      case JENKINS:
      case BAMBOO:
      case DOCKER:
      case NEXUS:
      case ARTIFACTORY:
        return settingValue.fetchRequiredExecutionCapabilities();
      default:
        return getExecutionCapabilitiesFromArtifactStreamType();
    }
  }

  private List<ExecutionCapability> getExecutionCapabilitiesFromArtifactStreamType() {
    if (artifactStreamType.equals(GCR.name())) {
      String gcrHostName = artifactStreamAttributes.getRegistryHostName();
      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getUrl(gcrHostName)));
    } else if (artifactStreamType.equals(AZURE_ARTIFACTS.name())) {
      return settingValue.fetchRequiredExecutionCapabilities();
    } else if (artifactStreamType.equals(ACR.name())) {
      final String default_server = "azure.microsoft.com";
      String loginServer = isNotEmpty(artifactStreamAttributes.getRegistryHostName())
          ? artifactStreamAttributes.getRegistryHostName()
          : default_server;
      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getUrl(loginServer)));
    } else {
      return Collections.singletonList(IgnoreValidationCapabilityGenerator.buildIgnoreValidationCapability());
    }
  }

  private String getUrl(String hostName) {
    return "https://" + hostName + (hostName.endsWith("/") ? "" : "/");
  }
}
