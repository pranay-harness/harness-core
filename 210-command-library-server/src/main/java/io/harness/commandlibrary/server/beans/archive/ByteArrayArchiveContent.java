/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.beans.archive;

import io.harness.exception.GeneralException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;

@Builder
public class ByteArrayArchiveContent implements ArchiveContent {
  @NonNull private final String path;
  @NonNull private final byte[] fileBytes;

  public String getPath() {
    return path;
  }

  public String string(final Charset encoding) {
    try {
      return IOUtils.toString(fileBytes, encoding.name());
    } catch (IOException e) {
      throw new GeneralException("Error while getting string content", e);
    }
  }

  public InputStream byteStream() {
    return new ByteArrayInputStream(fileBytes);
  }
}
