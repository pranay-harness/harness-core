/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.beans.archive;

import java.util.Collection;
import java.util.Optional;

public interface ArchiveFile {
  Optional<ArchiveContent> getContent(String path);

  Collection<ArchiveContent> allContent();
}
