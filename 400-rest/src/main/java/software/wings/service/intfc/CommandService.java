/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;

/**
 * Created by peeyushaggarwal on 11/16/16.
 */
@OwnedBy(CDC)
public interface CommandService {
  Command getCommand(String appId, String originEntityId, int version);
  ServiceCommand getServiceCommand(String appId, String serviceCommandId);
  ServiceCommand getServiceCommandByName(String appId, String serviceId, String serviceCommandName);
  Command save(Command command, boolean pushToYaml);
}
