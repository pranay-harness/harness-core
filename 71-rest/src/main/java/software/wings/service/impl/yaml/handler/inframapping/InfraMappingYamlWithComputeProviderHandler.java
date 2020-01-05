package software.wings.service.impl.yaml.handler.inframapping;

import io.harness.exception.HarnessException;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.yaml.ChangeContext;

/**
 * @author rktummala on 10/15/17
 */
public abstract class InfraMappingYamlWithComputeProviderHandler<
    Y extends InfrastructureMapping.YamlWithComputeProvider, B extends InfrastructureMapping>
    extends InfraMappingYamlHandler<Y, B> {
  @Override
  protected void toYaml(Y yaml, B infraMapping) {
    super.toYaml(yaml, infraMapping);
    yaml.setComputeProviderType(infraMapping.getComputeProviderType());
    yaml.setComputeProviderName(getSettingName(infraMapping.getComputeProviderSettingId()));
  }

  protected void toBean(ChangeContext<Y> context, B bean, String appId, String envId, String computeProviderId,
      String serviceId, String provisionerId) throws HarnessException {
    super.toBean(context, bean, appId, envId, serviceId, provisionerId);
    Y yaml = context.getYaml();
    bean.setComputeProviderSettingId(computeProviderId);
    bean.setComputeProviderName(yaml.getComputeProviderName());
    bean.setComputeProviderType(yaml.getComputeProviderType());
  }
}
