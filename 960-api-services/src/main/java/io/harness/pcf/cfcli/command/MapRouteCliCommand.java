/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pcf.cfcli.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommand;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.cfcli.option.Flag;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Option;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.CDP)
public class MapRouteCliCommand extends CfCliCommand {
  @Builder
  MapRouteCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      MapRouteOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.MAP_ROUTE, arguments, options);
  }

  @SuperBuilder
  public static class MapRouteOptions implements Options {
    @Option(value = "--hostname") String hostname;
    @Option(value = "--path") String path;
    @Option(value = "--port") String port;
  }

  @Value
  @SuperBuilder
  @EqualsAndHashCode(callSuper = true)
  public static class MapRouteOptionsV6 extends MapRouteOptions {
    @Flag(value = "--random-port") boolean randomPorts;
  }

  @Value
  @SuperBuilder
  @EqualsAndHashCode(callSuper = true)
  public static class MapRouteOptionsV7 extends MapRouteOptions {}
}
