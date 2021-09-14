/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.CloudProviderInfrastructure;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;

public abstract class CloudProviderInfrastructureYamlHandler<Y extends CloudProviderInfrastructureYaml, B
                                                                 extends InfraMappingInfrastructureProvider>
    extends BaseYamlHandler<Y, B> {
  @Override public void delete(ChangeContext<Y> changeContext){};
  @Override
  public B get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
  @Override public abstract Y toYaml(B bean, String appId);
  @Override public abstract B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);
}
