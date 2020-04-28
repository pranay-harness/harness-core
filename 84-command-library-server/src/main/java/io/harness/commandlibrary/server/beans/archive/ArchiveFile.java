package io.harness.commandlibrary.server.beans.archive;

import java.util.Collection;
import java.util.Optional;

public interface ArchiveFile {
  Optional<ArchiveContent> getContent(String path);

  Collection<ArchiveContent> allContent();
}
