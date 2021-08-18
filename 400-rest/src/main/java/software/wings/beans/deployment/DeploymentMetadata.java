package software.wings.beans.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.DeploymentType;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.EnvSummary;
import software.wings.beans.ManifestVariable;
import software.wings.beans.Service;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeploymentMetadataKeys")
@TargetModule(HarnessModule._957_CG_BEANS)
public class DeploymentMetadata {
  private List<Service> artifactRequiredServices = new ArrayList<>();
  private transient List<String> artifactRequiredServiceIds = new ArrayList<>();
  private List<EnvSummary> envSummaries = new ArrayList<>();
  private transient List<String> envIds = new ArrayList<>();
  private List<DeploymentType> deploymentTypes = new ArrayList<>();
  private List<ArtifactVariable> artifactVariables = new ArrayList<>();
  private List<ManifestVariable> manifestVariables = new ArrayList<>();
  private List<String> manifestRequiredServiceIds = new ArrayList<>();

  public enum Include { ENVIRONMENT, ARTIFACT_SERVICE, DEPLOYMENT_TYPE, LAST_DEPLOYED_ARTIFACT }
}
