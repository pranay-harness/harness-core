package software.wings.beans.deployment;

import lombok.Builder;
import lombok.Data;
import software.wings.api.DeploymentType;
import software.wings.beans.EnvSummary;
import software.wings.beans.Service;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DeploymentMetadata {
  private List<Service> artifactRequiredServices = new ArrayList<>();
  private transient List<String> artifactRequiredServiceIds = new ArrayList<>();
  private List<EnvSummary> envSummaries = new ArrayList<>();
  private transient List<String> envIds = new ArrayList<>();
  private List<DeploymentType> deploymentTypes = new ArrayList<>();

  public enum Include { ENVIRONMENT, ARTIFACT_SERVICE, DEPLOYMENT_TYPE }
}
