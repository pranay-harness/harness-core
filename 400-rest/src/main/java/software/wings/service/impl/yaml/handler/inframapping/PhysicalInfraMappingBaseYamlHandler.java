package software.wings.service.impl.yaml.handler.inframapping;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import software.wings.beans.PhysicalInfrastructureMappingBase;
import software.wings.beans.PhysicalInfrastructureMappingBase.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@Singleton
public abstract class PhysicalInfraMappingBaseYamlHandler<Y extends PhysicalInfrastructureMappingBase.Yaml, B
                                                              extends PhysicalInfrastructureMappingBase>
    extends InfraMappingYamlWithComputeProviderHandler<Y, B> {
  @Override
  public void toYaml(Y yaml, B bean) {
    super.toYaml(yaml, bean);
    yaml.setHostNames(bean.getHostNames());
    yaml.setLoadBalancer(bean.getLoadBalancerName());
  }

  @Override
  protected void toBean(ChangeContext<Y> changeContext, B bean, String appId, String envId, String computeProviderId,
      String serviceId, String provisionerId) {
    Yaml yaml = changeContext.getYaml();
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, provisionerId);
    if (isNotBlank(yaml.getLoadBalancer())) {
      bean.setLoadBalancerId(getSettingId(bean.getAccountId(), appId, yaml.getLoadBalancer()));
    } else {
      bean.setLoadBalancerId("");
    }

    bean.setHostNames(yaml.getHostNames());
  }
}
