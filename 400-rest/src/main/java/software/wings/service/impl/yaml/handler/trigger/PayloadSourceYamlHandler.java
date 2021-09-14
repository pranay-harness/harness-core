/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.trigger;

import io.harness.eraro.ErrorCode;
import io.harness.exception.TriggerException;

import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.yaml.trigger.PayloadSourceYaml;

import com.google.inject.Inject;
import java.util.List;

public abstract class PayloadSourceYamlHandler<Y extends PayloadSourceYaml> extends BaseYamlHandler<Y, PayloadSource> {
  @Inject protected YamlHelper yamlHelper;
  @Override
  public void delete(ChangeContext<Y> changeContext) {}
  @Override
  public PayloadSource get(String accountId, String yamlFilePath) {
    throw new TriggerException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override public abstract Y toYaml(PayloadSource bean, String appId);

  @Override
  public abstract PayloadSource upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);
}
