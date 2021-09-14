/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ProcessCheckStoppedCommandUnit;
import software.wings.beans.command.ProcessCheckStoppedCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class ProcessCheckStoppedCommandUnitYamlHandler
    extends AbstractExecCommandUnitYamlHandler<Yaml, ProcessCheckStoppedCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(ProcessCheckStoppedCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected ProcessCheckStoppedCommandUnit getCommandUnit() {
    return new ProcessCheckStoppedCommandUnit();
  }
}
