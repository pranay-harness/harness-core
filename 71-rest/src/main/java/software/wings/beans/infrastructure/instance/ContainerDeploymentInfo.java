package software.wings.beans.infrastructure.instance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.util.List;

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
  @Indexed private long lastVisited;

  /**
   * In case of ECS, this would be taskDefinitionArn
   * In case of Kubernetes, this would be replicationControllerName
   * This has the revision number in it.
   */
  @Indexed private String containerSvcName;
  @Indexed private String containerSvcNameNoRevision;

  @Builder
  public ContainerDeploymentInfo(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String accountId,
      String serviceId, String envId, String infraMappingId, String computeProviderId, String workflowId,
      String workflowExecutionId, String pipelineExecutionId, String stateExecutionInstanceId,
      InstanceType instanceType, String clusterName, long lastVisited, String containerSvcName,
      String containerSvcNameNoRevision) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
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
    this.lastVisited = lastVisited;
    this.containerSvcName = containerSvcName;
    this.containerSvcNameNoRevision = containerSvcNameNoRevision;
  }
}
