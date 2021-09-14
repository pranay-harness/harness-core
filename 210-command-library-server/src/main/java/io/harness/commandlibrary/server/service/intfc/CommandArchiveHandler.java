/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.service.intfc;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;

public interface CommandArchiveHandler {
  boolean supports(CommandArchiveContext commandArchiveContext);
  String createNewCommandVersion(CommandArchiveContext commandArchiveContext);
}
