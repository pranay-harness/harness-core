/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.AwsLambdaCommandUnit.Yaml;

import com.google.inject.Singleton;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
@OwnedBy(CDP)
public class AwsLambdaCommandUnitYamlHandler
    extends CommandUnitYamlHandler<AwsLambdaCommandUnit.Yaml, AwsLambdaCommandUnit> {
  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Yaml toYaml(AwsLambdaCommandUnit bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    return yaml;
  }

  @Override
  protected AwsLambdaCommandUnit getCommandUnit() {
    return new AwsLambdaCommandUnit();
  }
}
