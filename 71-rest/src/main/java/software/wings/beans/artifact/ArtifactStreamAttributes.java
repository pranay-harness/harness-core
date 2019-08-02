package software.wings.beans.artifact;

import com.google.common.collect.Maps;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@ToString(exclude = {"serverSetting", "artifactServerEncryptedDataDetails"})
public class ArtifactStreamAttributes implements ExecutionCapabilityDemander {
  private String jobName;
  private String imageName;
  private String registryHostName;
  private String subscriptionId;
  private String registryName;
  private String repositoryName;
  private String artifactStreamType;
  private SettingAttribute serverSetting;
  // TODO : Refactoring has to be done
  private String groupId; // For nexus integration
  private String artifactStreamId;
  private String artifactName;
  private ArtifactType artifactType;
  private String artifactPattern;
  private String region;
  private String repositoryType;
  private boolean metadataOnly;
  private Map<String, List<String>> tags;
  private String platform;
  private Map<String, String> filters;
  private List<EncryptedDataDetail> artifactServerEncryptedDataDetails;
  private Map<String, String> metadata = Maps.newHashMap();
  private boolean copyArtifactEnabledForArtifactory;
  private String artifactoryDockerRepositoryServer;
  private String nexusDockerPort;
  private String nexusDockerRegistryUrl;
  private String nexusPackageName;
  private String repositoryFormat;
  private String customScriptTimeout;
  private String accountId;
  private String customArtifactStreamScript;
  private String artifactRoot;
  private String buildNoPath;
  private Map<String, String> artifactAttributes;
  private boolean customAttributeMappingNeeded;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (registryHostName != null) {
      executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          "https://" + registryHostName + (registryHostName.endsWith("/") ? "" : "/")));
    }
    executionCapabilities.addAll(CapabilityHelper.generateKmsHttpCapabilities(artifactServerEncryptedDataDetails));
    return executionCapabilities;
  }
}
