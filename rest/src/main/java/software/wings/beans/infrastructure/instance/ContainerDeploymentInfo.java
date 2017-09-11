package software.wings.beans.infrastructure.instance;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.infrastructure.instance.key.InstanceKey;

/**
 * Represents the deployment info of the ECS / kubernetes container.
 * Once the instances are synced, these values are used to store the instances.
 * @author rktummala on 09/05/17
 */
@Data
public abstract class ContainerDeploymentInfo extends Base {
  private String clusterName;

  private String envId;
  private String envName;
  private EnvironmentType envType;
  private String accountId;
  private String serviceId;
  private String serviceName;
  private String appName;
  private String infraMappingId;
  private String infraMappingType;
  private String computeProviderId;
  private String computeProviderName;

  private String lastArtifactStreamId;
  private String lastArtifactId;
  private String lastArtifactName;
  private String lastArtifactSourceName;
  protected String lastArtifactBuildNum;

  private String lastDeployedById;
  private String lastDeployedByName;
  private long lastDeployedAt;

  private String lastWorkflowId;
  private String lastWorkflowName;

  private String lastPipelineId;
  private String lastPipelineName;
}
