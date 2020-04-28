package io.harness.commandlibrary.server.beans.archive;

import lombok.Builder;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

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
