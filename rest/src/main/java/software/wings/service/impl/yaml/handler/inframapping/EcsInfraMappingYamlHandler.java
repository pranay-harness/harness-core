package software.wings.service.impl.yaml.handler.inframapping;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.LaunchType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class EcsInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, EcsInfrastructureMapping> {
  @Override
  public Yaml toYaml(EcsInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.AWS_ECS.name());
    yaml.setRegion(bean.getRegion());
    yaml.setCluster(bean.getClusterName());

    setFargateData(bean, yaml);
    return yaml;
  }

  @Override
  public EcsInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId);

    EcsInfrastructureMapping current = new EcsInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    EcsInfrastructureMapping previous =
        (EcsInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (EcsInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (EcsInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private void toBean(EcsInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId);
    bean.setRegion(yaml.getRegion());
    bean.setClusterName(yaml.getCluster());
    if (isBlank(yaml.getLaunchType())) {
      bean.setLaunchType(LaunchType.EC2.name());
    } else {
      bean.setLaunchType(yaml.getLaunchType());
    }

    if (LaunchType.FARGATE.name().equals(yaml.getLaunchType())) {
      validateNetworkParamateres(yaml, bean);
    }

    bean.setVpcId(yaml.getVpcId());
    bean.setSubnetIds(getIdList(yaml.getSubnetIds()));
    bean.setSecurityGroupIds(getIdList(yaml.getSecurityGroupIds()));
    bean.setAssignPublicIp(yaml.isAssignPublicIp());
  }

  private List<String> getIdList(String ids) {
    if (isBlank(ids)) {
      return Collections.EMPTY_LIST;
    }

    String[] idArr = ids.split(",");
    return Arrays.asList(idArr);
  }

  private void validateNetworkParamateres(Yaml yaml, EcsInfrastructureMapping bean) {
    if (isBlank(yaml.getVpcId()) || isBlank(yaml.getSubnetIds()) || isBlank(yaml.getSecurityGroupIds())) {
      StringBuilder builder = new StringBuilder(128);
      builder.append("Failed to parse yaml for EcsInfraMapping: ")
          .append(bean.getName())
          .append(", App: ")
          .append(bean.getAppId())
          .append(", For Fargate Lauch type, VpcId  -  SubnetIds  - SecurityGroupIds are required, can not be blank");
      throw new WingsException(builder.toString());
    }
  }

  @Override
  public EcsInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (EcsInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  private void setFargateData(EcsInfrastructureMapping bean, Yaml yaml) {
    if (isBlank(bean.getLaunchType())) {
      yaml.setLaunchType(LaunchType.EC2.name());
    } else {
      yaml.setLaunchType(bean.getLaunchType());
    }

    yaml.setVpcId(bean.getVpcId());
    yaml.setSubnetIds(getIds(bean.getSubnetIds()));
    yaml.setSecurityGroupIds(getIds(bean.getSecurityGroupIds()));
    yaml.setAssignPublicIp(bean.isAssignPublicIp());
  }

  private String getIds(List<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return StringUtils.EMPTY;
    }

    StringBuilder builder = new StringBuilder();
    return String.join(",", ids);
  }
}
