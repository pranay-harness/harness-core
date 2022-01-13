/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.utils;

import io.harness.commandlibrary.server.beans.archive.ArchiveContent;
import io.harness.commandlibrary.server.beans.archive.ArchiveFile;
import io.harness.commandlibrary.server.beans.archive.ArchiveFileImpl;
import io.harness.commandlibrary.server.beans.archive.ByteArrayArchiveContent;
import io.harness.exception.FileReadException;
import io.harness.exception.GeneralException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;

@UtilityClass
public class ArchiveUtils {
  public static ArchiveFile createArchiveFile(InputStream inputStream) {
    return ArchiveFileImpl.builder().fileToContentMap(readArchiveEntries(inputStream)).build();
  }

  public static Collection<String> getAllFilePaths(ArchiveFile archiveFile) {
    return archiveFile.allContent().stream().map(ArchiveContent::getPath).collect(Collectors.toList());
  }

  private static Map<String, ArchiveContent> readArchiveEntries(InputStream inputStream) {
    final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
    final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory(StandardCharsets.UTF_8.name());
    final Map<String, ArchiveContent> fileNameToContentMap = new HashMap<>();

    try (ArchiveInputStream archiveInputStream = archiveStreamFactory.createArchiveInputStream(bufferedInputStream)) {
      ArchiveEntry archiveEntry;
      while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
        fileNameToContentMap.put(archiveEntry.getName(), newArchiveContentFrom(archiveEntry, archiveInputStream));
      }
      return fileNameToContentMap;

    } catch (Exception e) {
      throw new GeneralException("Error while reading archive file", e);
    }
  }

  private static ArchiveContent newArchiveContentFrom(
      ArchiveEntry archiveEntry, ArchiveInputStream archiveInputStream) {
    try {
      return ByteArrayArchiveContent.builder()
          .path(archiveEntry.getName())
          .fileBytes(IOUtils.toByteArray(archiveInputStream))
          .build();
    } catch (IOException e) {
      throw new FileReadException("error while reading file", e);
    }
  }
}
