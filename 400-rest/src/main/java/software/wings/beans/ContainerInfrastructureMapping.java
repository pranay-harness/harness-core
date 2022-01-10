/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;

import software.wings.annotation.Blueprint;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Created by rishi on 5/18/17.
 */
@FieldNameConstants(innerTypeName = "ContainerInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public abstract class ContainerInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Cluster Name") @Blueprint private String clusterName;

  /**
   * Instantiates a new Infrastructure mapping.
   *
   * @param infraMappingType the infra mapping type
   */
  public ContainerInfrastructureMapping(String infraMappingType) {
    super(infraMappingType);
  }

  public ContainerInfrastructureMapping(String entityYamlPath, String appId, String accountId, String type, String uuid,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, boolean autoPopulateName, Map<String, Object> blueprints, String clusterName, String provisionerId,
      boolean sample) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, autoPopulateName, blueprints, provisionerId, sample);
    this.clusterName = clusterName;
  }

  /**
   * Gets cluster name.
   *
   * @return the cluster name
   */
  @Attributes(title = "Cluster Name")
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Sets cluster name.
   *
   * @param clusterName the cluster name
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends InfraMappingYaml {
    private String cluster;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String cluster, Map<String, Object> blueprints) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, blueprints);
      this.cluster = cluster;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class YamlWithComputeProvider extends software.wings.beans.YamlWithComputeProvider {
    private String cluster;

    public YamlWithComputeProvider(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, String cluster,
        Map<String, Object> blueprints) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName, blueprints);
      this.cluster = cluster;
    }
  }

  public abstract String getNamespace();

  public abstract String getReleaseName();

  public abstract void setReleaseName(String releaseName);
}
