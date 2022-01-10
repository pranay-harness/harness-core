/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
