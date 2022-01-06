/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.stream;

import static io.harness.eraro.ErrorCode.FILE_DOWNLOAD_FAILED;
import static io.harness.eraro.ErrorCode.INVALID_URL;

import static java.lang.String.format;

import io.harness.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class BoundedInputStream extends InputStream {
  private final InputStream inputStream;
  private final long size;
  private long totalBytesRead;

  public BoundedInputStream(InputStream inputStream) {
    this(inputStream, -1); // no size limit imposed
  }

  public BoundedInputStream(InputStream inputStream, long size) {
    this.inputStream = inputStream;
    this.size = size;
  }

  public static BoundedInputStream createBoundedStreamForUrl(String urlString, long size) {
    try {
      URL url = new URL(urlString);
      return new BoundedInputStream(url.openStream(), size);
    } catch (MalformedURLException ex) {
      throw new WingsException(INVALID_URL);
    } catch (IOException ex) {
      throw new WingsException(FILE_DOWNLOAD_FAILED);
    }
  }

  private int updateTotalBytesRead(int bytesRead) throws IOException {
    if (bytesRead > 0) {
      totalBytesRead += bytesRead;
      if (size >= 0 && totalBytesRead > size) {
        throw new IOException(format("Input stream size limit exceeded. Allowed limit %s bytes", size));
      }
    }
    return bytesRead;
  }

  @Override
  public int read() throws IOException {
    int numBytes = inputStream.read();
    return updateTotalBytesRead(numBytes == -1 ? -1 : 1);
  }

  @Override
  public int read(byte bytes[]) throws IOException {
    return read(bytes, 0, bytes.length);
  }

  @Override
  public int read(byte bytes[], int off, int len) throws IOException {
    return updateTotalBytesRead(inputStream.read(bytes, off, len));
  }

  public long getTotalBytesRead() {
    return totalBytesRead;
  }

  public long getSize() {
    return size;
  }
}
