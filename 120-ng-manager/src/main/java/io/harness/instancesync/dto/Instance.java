package io.harness.instancesync.dto;

import io.harness.beans.EnvironmentType;
import io.harness.instancesync.dto.instanceinfo.InstanceInfo;

import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.beans.infrastructure.instance.key.PodInstanceKey;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
public class Instance {
  @NotEmpty private InstanceType instanceType;
  private HostInstanceKey hostInstanceKey;
  private ContainerInstanceKey containerInstanceKey;
  private PcfInstanceKey pcfInstanceKey;
  private PodInstanceKey podInstanceKey;

  private String envId;
  private String envName;
  private EnvironmentType envType;
  private String accountId;
  private String serviceId;
  private String serviceName;
  private String appName;

  private String infraMappingType;

  private String connectorId;

  private String lastArtifactId;
  private String lastArtifactName;
  private String lastArtifactBuildNum;

  private String lastDeployedById;
  private String lastDeployedByName;
  private long lastDeployedAt;

  private String lastPipelineExecutionId;
  private String lastPipelineExecutionName;

  private InstanceInfo instanceInfo;

  private boolean isDeleted;
  private long deletedAt;

  private boolean needRetry;
}
