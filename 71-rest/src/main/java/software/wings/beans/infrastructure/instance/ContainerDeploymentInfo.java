package software.wings.beans.infrastructure.instance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

/**
 *
 * @author rktummala on 09/13/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "containerDeploymentInfo", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentInfo extends Base {
  private String accountId;
  private String serviceId;
  private String envId;
  private String infraMappingId;
  private String computeProviderId;
  private String workflowId;
  private String workflowExecutionId;
  private String pipelineExecutionId;
  private String stateExecutionInstanceId;
  private InstanceType instanceType;
  private String clusterName;
  private String namespace;
  private long lastVisited;

  /**
   * In case of ECS, this would be taskDefinitionArn
   * In case of Kubernetes, this would be replicationControllerName
   * This has the revision number in it.
   */
  private String containerSvcName;
  @Indexed private String containerSvcNameNoRevision;

  @Builder
  public ContainerDeploymentInfo(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String accountId, String serviceId,
      String envId, String infraMappingId, String computeProviderId, String workflowId, String workflowExecutionId,
      String pipelineExecutionId, String stateExecutionInstanceId, InstanceType instanceType, String clusterName,
      String namespace, long lastVisited, String containerSvcName, String containerSvcNameNoRevision) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.accountId = accountId;
    this.serviceId = serviceId;
    this.envId = envId;
    this.infraMappingId = infraMappingId;
    this.computeProviderId = computeProviderId;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.pipelineExecutionId = pipelineExecutionId;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
    this.instanceType = instanceType;
    this.clusterName = clusterName;
    this.namespace = namespace;
    this.lastVisited = lastVisited;
    this.containerSvcName = containerSvcName;
    this.containerSvcNameNoRevision = containerSvcNameNoRevision;
  }
}
