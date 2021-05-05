package io.harness.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.dto.instanceinfo.InstanceInfo;

import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.beans.infrastructure.instance.key.PodInstanceKey;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@OwnedBy(HarnessTeam.DX)
public class Instance {
  public static final String ACCOUNT_ID_FIELD = "accountId";
  public static final String CREATED_AT_FIELD = "createdAt";
  public static final String IS_DELETED_FIELD = "isDeleted";
  public static final String DELETED_AT_FIELD = "deletedAt";

  private String accountId;
  private String orgId;
  private String projectId;
  @NotEmpty private InstanceType instanceType;
  private String infrastructureMappingId;
  private HostInstanceKey hostInstanceKey;
  private ContainerInstanceKey containerInstanceKey;
  private PcfInstanceKey pcfInstanceKey;
  private PodInstanceKey podInstanceKey;

  private String envId;
  private String envName;
  private EnvironmentType envType;

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
