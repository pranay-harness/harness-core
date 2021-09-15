/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.AmiCommandUnit;
import software.wings.beans.command.AmiCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 12/29/17
 */
@Singleton
public class AmiCommandUnitYamlHandler extends CommandUnitYamlHandler<Yaml, AmiCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(AmiCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected AmiCommandUnit getCommandUnit() {
    return new AmiCommandUnit();
  }
}
