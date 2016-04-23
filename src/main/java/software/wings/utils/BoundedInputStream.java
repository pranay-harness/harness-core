package software.wings.utils;

import static java.lang.String.format;
import static software.wings.beans.ErrorConstants.FILE_DOWNLOAD_FAILED;
import static software.wings.beans.ErrorConstants.INVALID_URL;

import software.wings.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

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
  public int read(byte bytes[]) throws IOException {
    return read(bytes, 0, bytes.length);
  }

  @Override
  public int read(byte bytes[], int off, int len) throws IOException {
    return updateTotalBytesRead(inputStream.read(bytes, off, len));
  }

  public static BoundedInputStream getBoundedStreamForUrl(String urlString, int size) {
    try {
      URL url = new URL(urlString);
      return new BoundedInputStream(url.openStream(), size);
    } catch (MalformedURLException ex) {
      throw new WingsException(INVALID_URL);
    } catch (IOException ex) {
      throw new WingsException(FILE_DOWNLOAD_FAILED);
    }
  }
}
