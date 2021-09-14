/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.KubernetesResizeCommandUnit;
import software.wings.beans.command.KubernetesResizeCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class KubernetesResizeCommandUnitYamlHandler
    extends ContainerResizeCommandUnitYamlHandler<Yaml, KubernetesResizeCommandUnit> {
  @Override
  public Class getYamlClass() {
    return KubernetesResizeCommandUnit.Yaml.class;
  }

  @Override
  public Yaml toYaml(KubernetesResizeCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected KubernetesResizeCommandUnit getCommandUnit() {
    return new KubernetesResizeCommandUnit();
  }
}
