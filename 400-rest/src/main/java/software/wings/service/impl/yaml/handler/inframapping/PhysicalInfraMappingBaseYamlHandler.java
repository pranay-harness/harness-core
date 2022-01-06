/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.PhysicalInfrastructureMappingBase;
import software.wings.beans.PhysicalInfrastructureMappingBase.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
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
