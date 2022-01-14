package io.harness.commandlibrary.server.service.intfc;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;

public interface CommandArchiveHandler {
  boolean supports(CommandArchiveContext commandArchiveContext);
  String createNewCommandVersion(CommandArchiveContext commandArchiveContext);
}
