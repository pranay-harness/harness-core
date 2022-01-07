/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.CustomPayloadSource;
import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.CustomPayloadSourceYaml;

import java.util.List;

@OwnedBy(CDC)
public class CustomPayloadSourceYamlHandler extends PayloadSourceYamlHandler<CustomPayloadSourceYaml> {
  @Override
  public CustomPayloadSourceYaml toYaml(PayloadSource bean, String appId) {
    return CustomPayloadSourceYaml.builder().build();
  }

  @Override
  public PayloadSource upsertFromYaml(
      ChangeContext<CustomPayloadSourceYaml> changeContext, List<ChangeContext> changeSetContext) {
    return CustomPayloadSource.builder().build();
  }

  @Override
  public Class getYamlClass() {
    return CustomPayloadSourceYaml.class;
  }
}
