/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.beans.archive;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;

@Builder
public class ArchiveFileImpl implements ArchiveFile {
  private final Map<String, ArchiveContent> fileToContentMap;

  public Optional<ArchiveContent> getContent(String path) {
    return Optional.ofNullable(fileToContentMap.get(path));
  }

  @Override
  public Collection<ArchiveContent> allContent() {
    return fileToContentMap.values();
  }
}
