package io.harness.commandlibrary.server.beans;

import io.harness.commandlibrary.server.beans.archive.ArchiveFile;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandArchiveContext {
  String commandStoreName;
  CommandManifest commandManifest;
  ArchiveFile archiveFile;
  String accountId;
}
