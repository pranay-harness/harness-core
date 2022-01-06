/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
