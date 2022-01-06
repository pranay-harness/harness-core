/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.WingsException;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Common methods needed by both instance and container instance.
 * This had to be created to avoid a cyclic dependency between InstanceHelper/ContainerInstanceHelper and
 * InstanceServiceImpl.
 * @author rktummala on 09/11/17
 */
@Singleton
@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule._441_CG_INSTANCE_SYNC)
public class InstanceUtils {
  private static final String WORKFLOW_PREFIX = "Workflow: ";
  private static final int WORKFLOW_PREFIX_LENGTH = 10;

  public void setInstanceType(InstanceBuilder builder, String infraMappingType) {
    builder.instanceType(getInstanceType(infraMappingType));
  }

  public InstanceType getInstanceType(String infraMappingType) {
    InstanceType instanceType;
    if (InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.AZURE_KUBERNETES.name().equals(infraMappingType)
        || InfrastructureMappingType.GCP_KUBERNETES.name().equals(infraMappingType)) {
      instanceType = InstanceType.KUBERNETES_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.AWS_ECS.name().equals(infraMappingType)) {
      instanceType = InstanceType.ECS_CONTAINER_INSTANCE;
    } else if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM.name().equals(infraMappingType)
        || InfrastructureMappingType.CUSTOM.name().equals(infraMappingType)) {
      instanceType = InstanceType.PHYSICAL_HOST_INSTANCE;
    } else if (InfrastructureMappingType.AWS_SSH.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name().equals(infraMappingType)
        || InfrastructureMappingType.AWS_AMI.name().equals(infraMappingType)) {
      instanceType = InstanceType.EC2_CLOUD_INSTANCE;
    } else if (InfrastructureMappingType.PCF_PCF.name().equals(infraMappingType)) {
      instanceType = InstanceType.PCF_INSTANCE;
    } else if (InfrastructureMappingType.AZURE_VMSS.name().equals(infraMappingType)) {
      instanceType = InstanceType.AZURE_VMSS_INSTANCE;
    } else if (InfrastructureMappingType.AZURE_WEBAPP.name().equals(infraMappingType)) {
      instanceType = InstanceType.AZURE_WEB_APP_INSTANCE;
    } else if (InfrastructureMappingType.AZURE_INFRA.name().equals(infraMappingType)) {
      instanceType = InstanceType.AZURE_VMSS_INSTANCE;
    } else {
      String msg = "Unsupported infraMapping type:" + infraMappingType;
      log.error(msg);
      throw new WingsException(msg);
    }

    return instanceType;
  }

  public String getWorkflowName(String workflowName) {
    if (workflowName == null) {
      return null;
    }

    if (workflowName.startsWith(WORKFLOW_PREFIX)) {
      return workflowName.substring(WORKFLOW_PREFIX_LENGTH);
    } else {
      return workflowName;
    }
  }
}
