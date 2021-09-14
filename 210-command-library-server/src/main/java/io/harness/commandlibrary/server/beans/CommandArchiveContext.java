/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
