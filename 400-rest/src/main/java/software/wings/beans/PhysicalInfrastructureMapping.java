/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.beans.infrastructure.Host;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
@FieldNameConstants(innerTypeName = "PhysicalInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class PhysicalInfrastructureMapping extends PhysicalInfrastructureMappingBase {
  private String hostConnectionAttrs;

  public PhysicalInfrastructureMapping() {
    super(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH);
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> blueprintProperties, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    if (isEmpty(blueprintProperties)) {
      throw new InvalidRequestException("Infra Provisioner Mapping inputs can't be empty");
    }
    List<Map<String, Object>> hostList = (List<Map<String, Object>>) blueprintProperties.get("hostArrayPath");
    if (hostList == null) {
      throw new InvalidRequestException("Host array path not found");
    }
    List<Host> hosts = new ArrayList<>();

    for (Map<String, Object> hostAttributes : hostList) {
      Host host = Host.Builder.aHost()
                      .withAppId(getAppId())
                      .withEnvId(getEnvId())
                      .withHostConnAttr(getHostConnectionAttrs())
                      .withInfraMappingId(getUuid())
                      .withServiceTemplateId(getServiceTemplateId())
                      .withProperties(new HashMap<>())
                      .build();

      for (Entry<String, Object> entry : hostAttributes.entrySet()) {
        switch (entry.getKey()) {
          case "Hostname":
          case "hostname":
            host.setHostName((String) hostAttributes.get(entry.getKey()));
            host.setPublicDns((String) hostAttributes.get(entry.getKey()));
            break;
          default:
            host.getProperties().put(entry.getKey(), hostAttributes.get(entry.getKey()));
        }
      }
      if (isEmpty(host.getPublicDns())) {
        throw new InvalidRequestException("Hostname can't be empty");
      }
      hosts.add(host);
    }
    if (isEmpty(hosts)) {
      throw new InvalidRequestException("Host list can't be empty", WingsException.USER);
    }
    hosts(hosts);
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(
        format("%s (DataCenter_SSH)", Optional.ofNullable(this.getComputeProviderName()).orElse("data-center-ssh")));
  }

  @Override
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends PhysicalInfrastructureMappingBase.Yaml {
    // maps to hostConnectionAttrs
    // This would either be a username/password / ssh key id
    private String connection;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String name, List<String> hostNames,
        String loadBalancer, String connection, List<Host> hosts, Map<String, Object> blueprints) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, name, hostNames, loadBalancer, hosts, blueprints);
      this.connection = connection;
    }
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    protected String accountId;
    private String hostConnectionAttrs;
    private List<String> hostNames;
    private String loadBalancerName;
    private String loadBalancerId;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String name;
    private String provisionerId;
    private boolean autoPopulate = true;
    private List<Host> hosts;

    private Builder() {}

    public static PhysicalInfrastructureMapping.Builder aPhysicalInfrastructureMapping() {
      return new PhysicalInfrastructureMapping.Builder();
    }

    public PhysicalInfrastructureMapping.Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      this.hostConnectionAttrs = hostConnectionAttrs;
      return this;
    }

    public Builder withProvisionerId(String provisionerId) {
      this.provisionerId = provisionerId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLoadBalancerId(String loadBalancerId) {
      this.loadBalancerId = loadBalancerId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withName(String name) {
      this.name = name;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public PhysicalInfrastructureMapping.Builder withHosts(List<Host> hosts) {
      this.hosts = hosts;
      return this;
    }

    public PhysicalInfrastructureMapping build() {
      PhysicalInfrastructureMapping physicalInfrastructureMapping = new PhysicalInfrastructureMapping();
      physicalInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      physicalInfrastructureMapping.setHostNames(hostNames);
      physicalInfrastructureMapping.setLoadBalancerName(loadBalancerName);
      physicalInfrastructureMapping.setLoadBalancerId(loadBalancerId);
      physicalInfrastructureMapping.setUuid(uuid);
      physicalInfrastructureMapping.setAppId(appId);
      physicalInfrastructureMapping.setAccountId(accountId);
      physicalInfrastructureMapping.setCreatedBy(createdBy);
      physicalInfrastructureMapping.setCreatedAt(createdAt);
      physicalInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      physicalInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      physicalInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      physicalInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      physicalInfrastructureMapping.setEnvId(envId);
      physicalInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      physicalInfrastructureMapping.setServiceId(serviceId);
      physicalInfrastructureMapping.setComputeProviderType(computeProviderType);
      physicalInfrastructureMapping.setInfraMappingType(infraMappingType);
      physicalInfrastructureMapping.setDeploymentType(deploymentType);
      physicalInfrastructureMapping.setComputeProviderName(computeProviderName);
      physicalInfrastructureMapping.setName(name);
      physicalInfrastructureMapping.setAutoPopulate(autoPopulate);
      physicalInfrastructureMapping.setAccountId(accountId);
      physicalInfrastructureMapping.hosts(hosts);
      physicalInfrastructureMapping.setProvisionerId(provisionerId);
      return physicalInfrastructureMapping;
    }
  }
}
