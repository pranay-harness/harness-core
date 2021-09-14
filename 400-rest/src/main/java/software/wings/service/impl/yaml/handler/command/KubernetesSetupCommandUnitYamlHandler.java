/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesSetupCommandUnit;
import software.wings.beans.command.KubernetesSetupCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author brett on 11/28/17
 */
@Singleton
public class KubernetesSetupCommandUnitYamlHandler
    extends ContainerSetupCommandUnitYamlHandler<Yaml, KubernetesSetupCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(KubernetesSetupCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected KubernetesSetupCommandUnit getCommandUnit() {
    return new KubernetesSetupCommandUnit();
  }
}
