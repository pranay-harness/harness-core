package software.wings.utils;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

/**
 * Created by anubhaw on 4/14/16.
 */

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

  private int updateTotalBytesRead(int bytesRead) throws IOException {
    if (bytesRead > 0) {
      totalBytesRead += bytesRead;
      if (size >= 0 && totalBytesRead > size) {
        throw new IOException(format("Inputstream size limit exceeded. Allowed limit %s bytes", size));
      }
    }
    return bytesRead;
  }

  @Override
  public int read() throws IOException {
    return updateTotalBytesRead(inputStream.read());
  }

  @Override
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    return updateTotalBytesRead(inputStream.read(b, off, len));
  }
}
