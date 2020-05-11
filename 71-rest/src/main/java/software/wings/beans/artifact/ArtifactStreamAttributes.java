package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
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
  private String groupId;
  private String artifactId;
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
  private Map<String, String> metadata = new HashMap<>();
  private List<ArtifactFileMetadata> artifactFileMetadata = new ArrayList<>();
  private boolean copyArtifactEnabled;
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
  private String extension;
  private String classifier;
  private String protocolType;
  private String project;
  private String feed;
  private String packageId;
  private String packageName;
  private List<String> artifactPaths;

  // These fields are used only during artifact collection and cleanup.
  private boolean isCollection;
  private Set<String> savedBuildDetailsKeys;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (registryHostName != null) {
      executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          "https://" + registryHostName + (registryHostName.endsWith("/") ? "" : "/")));
    }
    executionCapabilities.addAll(
        CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(artifactServerEncryptedDataDetails));
    return executionCapabilities;
  }
}
