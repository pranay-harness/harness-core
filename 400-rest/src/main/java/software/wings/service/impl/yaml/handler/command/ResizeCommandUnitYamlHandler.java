/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ResizeCommandUnit;
import software.wings.beans.command.ResizeCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ResizeCommandUnitYamlHandler extends ContainerResizeCommandUnitYamlHandler<Yaml, ResizeCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(ResizeCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ResizeCommandUnit getCommandUnit() {
    return new ResizeCommandUnit();
  }
}
