/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;

/**
 * Created by anubhaw on 6/2/16.
 */
@OwnedBy(CDC)
public interface ServiceCommandExecutorService {
  /**
   * Execute.
   *
   * @param command         the command
   * @param context         the context
   * @return the execution result
   */
  CommandExecutionStatus execute(Command command, CommandExecutionContext context);
}
