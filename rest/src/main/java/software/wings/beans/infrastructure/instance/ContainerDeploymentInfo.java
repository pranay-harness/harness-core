package software.wings.beans.infrastructure.instance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

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

  public static final class Builder {
    protected String appId;
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
    private long lastVisited;
    private String containerSvcName;
    private String containerSvcNameNoRevision;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aContainerDeploymentInfo() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public Builder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
      return this;
    }

    public Builder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public Builder withWorkflowExecutionId(String workflowExecutionId) {
      this.workflowExecutionId = workflowExecutionId;
      return this;
    }

    public Builder withPipelineExecutionId(String pipelineExecutionId) {
      this.pipelineExecutionId = pipelineExecutionId;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withInstanceType(InstanceType instanceType) {
      this.instanceType = instanceType;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withLastVisited(long lastVisited) {
      this.lastVisited = lastVisited;
      return this;
    }

    public Builder withContainerSvcName(String containerSvcName) {
      this.containerSvcName = containerSvcName;
      return this;
    }

    public Builder withContainerSvcNameNoRevision(String containerSvcNameNoRevision) {
      this.containerSvcNameNoRevision = containerSvcNameNoRevision;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return aContainerDeploymentInfo()
          .withAccountId(accountId)
          .withServiceId(serviceId)
          .withEnvId(envId)
          .withInfraMappingId(infraMappingId)
          .withComputeProviderId(computeProviderId)
          .withWorkflowId(workflowId)
          .withWorkflowExecutionId(workflowExecutionId)
          .withPipelineExecutionId(pipelineExecutionId)
          .withStateExecutionInstanceId(stateExecutionInstanceId)
          .withInstanceType(instanceType)
          .withClusterName(clusterName)
          .withLastVisited(lastVisited)
          .withContainerSvcName(containerSvcName)
          .withContainerSvcNameNoRevision(containerSvcNameNoRevision)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public ContainerDeploymentInfo build() {
      ContainerDeploymentInfo containerDeploymentInfo = new ContainerDeploymentInfo();
      containerDeploymentInfo.setAccountId(accountId);
      containerDeploymentInfo.setServiceId(serviceId);
      containerDeploymentInfo.setEnvId(envId);
      containerDeploymentInfo.setInfraMappingId(infraMappingId);
      containerDeploymentInfo.setComputeProviderId(computeProviderId);
      containerDeploymentInfo.setWorkflowId(workflowId);
      containerDeploymentInfo.setWorkflowExecutionId(workflowExecutionId);
      containerDeploymentInfo.setPipelineExecutionId(pipelineExecutionId);
      containerDeploymentInfo.setStateExecutionInstanceId(stateExecutionInstanceId);
      containerDeploymentInfo.setInstanceType(instanceType);
      containerDeploymentInfo.setClusterName(clusterName);
      containerDeploymentInfo.setLastVisited(lastVisited);
      containerDeploymentInfo.setContainerSvcName(containerSvcName);
      containerDeploymentInfo.setContainerSvcNameNoRevision(containerSvcNameNoRevision);
      containerDeploymentInfo.setUuid(uuid);
      containerDeploymentInfo.setAppId(appId);
      containerDeploymentInfo.setCreatedBy(createdBy);
      containerDeploymentInfo.setCreatedAt(createdAt);
      containerDeploymentInfo.setLastUpdatedBy(lastUpdatedBy);
      containerDeploymentInfo.setLastUpdatedAt(lastUpdatedAt);
      return containerDeploymentInfo;
    }
  }
}
