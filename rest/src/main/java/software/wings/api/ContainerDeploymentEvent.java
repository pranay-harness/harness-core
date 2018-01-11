package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.core.queue.Queuable;

import java.util.Date;
import java.util.Set;

/**
 * This is a wrapper class of ContainerDeploymentInfo to make it extend queuable.
 * This is used as request for capturing instance information.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "containerDeploymentQueue", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainerDeploymentEvent extends Queuable {
  private String appId;
  private String accountId;
  private String serviceId;
  private String envId;
  private String infraMappingId;
  private String computeProviderId;
  private String workflowId;
  private String workflowExecutionId;
  private String pipelineExecutionId;
  private String stateExecutionInstanceId;
  private String clusterName;
  private InstanceType instanceType;
  private String containerSvcNameNoRevision;

  /**
   * In case of ECS, this would be a list of taskDefinitionArns belonging to the same family (in other words, same
   * containerSvcNameNoRevision). In case of Kubernetes, this would be a list of replicationControllerNames belonging to
   * the same family (in other words, same containerSvcNameNoRevision). This has the revision number in it.
   */
  private Set<String> containerSvcNameSet;

  public static final class Builder {
    private String appId;
    private String accountId;
    private String serviceId;
    private String envId;
    private String infraMappingId;
    private String computeProviderId;
    private String workflowId;
    private String workflowExecutionId;
    private String id;
    private String pipelineExecutionId;
    private boolean running;
    private String stateExecutionInstanceId;
    private Date resetTimestamp = new Date(Long.MAX_VALUE);
    private String clusterName;
    private Date earliestGet = new Date();
    private InstanceType instanceType;
    private double priority;
    private String containerSvcNameNoRevision;
    private Date created = new Date();
    private int retries;
    private Set<String> containerSvcNameSet;

    private Builder() {}

    public static Builder aContainerDeploymentEvent() {
      return new Builder();
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
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

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withPipelineExecutionId(String pipelineExecutionId) {
      this.pipelineExecutionId = pipelineExecutionId;
      return this;
    }

    public Builder withRunning(boolean running) {
      this.running = running;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withResetTimestamp(Date resetTimestamp) {
      this.resetTimestamp = resetTimestamp;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withEarliestGet(Date earliestGet) {
      this.earliestGet = earliestGet;
      return this;
    }

    public Builder withInstanceType(InstanceType instanceType) {
      this.instanceType = instanceType;
      return this;
    }

    public Builder withPriority(double priority) {
      this.priority = priority;
      return this;
    }

    public Builder withContainerSvcNameNoRevision(String containerSvcNameNoRevision) {
      this.containerSvcNameNoRevision = containerSvcNameNoRevision;
      return this;
    }

    public Builder withCreated(Date created) {
      this.created = created;
      return this;
    }

    public Builder withRetries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder withContainerSvcNameSet(Set<String> containerSvcNameSet) {
      this.containerSvcNameSet = containerSvcNameSet;
      return this;
    }

    public Builder but() {
      return aContainerDeploymentEvent()
          .withAppId(appId)
          .withAccountId(accountId)
          .withServiceId(serviceId)
          .withEnvId(envId)
          .withInfraMappingId(infraMappingId)
          .withComputeProviderId(computeProviderId)
          .withWorkflowId(workflowId)
          .withWorkflowExecutionId(workflowExecutionId)
          .withId(id)
          .withPipelineExecutionId(pipelineExecutionId)
          .withRunning(running)
          .withStateExecutionInstanceId(stateExecutionInstanceId)
          .withResetTimestamp(resetTimestamp)
          .withClusterName(clusterName)
          .withEarliestGet(earliestGet)
          .withInstanceType(instanceType)
          .withPriority(priority)
          .withContainerSvcNameNoRevision(containerSvcNameNoRevision)
          .withCreated(created)
          .withRetries(retries)
          .withContainerSvcNameSet(containerSvcNameSet);
    }

    public ContainerDeploymentEvent build() {
      ContainerDeploymentEvent containerDeploymentEvent = new ContainerDeploymentEvent();
      containerDeploymentEvent.setAppId(appId);
      containerDeploymentEvent.setAccountId(accountId);
      containerDeploymentEvent.setServiceId(serviceId);
      containerDeploymentEvent.setEnvId(envId);
      containerDeploymentEvent.setInfraMappingId(infraMappingId);
      containerDeploymentEvent.setComputeProviderId(computeProviderId);
      containerDeploymentEvent.setWorkflowId(workflowId);
      containerDeploymentEvent.setWorkflowExecutionId(workflowExecutionId);
      containerDeploymentEvent.setId(id);
      containerDeploymentEvent.setPipelineExecutionId(pipelineExecutionId);
      containerDeploymentEvent.setRunning(running);
      containerDeploymentEvent.setStateExecutionInstanceId(stateExecutionInstanceId);
      containerDeploymentEvent.setResetTimestamp(resetTimestamp);
      containerDeploymentEvent.setClusterName(clusterName);
      containerDeploymentEvent.setEarliestGet(earliestGet);
      containerDeploymentEvent.setInstanceType(instanceType);
      containerDeploymentEvent.setPriority(priority);
      containerDeploymentEvent.setContainerSvcNameNoRevision(containerSvcNameNoRevision);
      containerDeploymentEvent.setCreated(created);
      containerDeploymentEvent.setRetries(retries);
      containerDeploymentEvent.setContainerSvcNameSet(containerSvcNameSet);
      return containerDeploymentEvent;
    }
  }
}
