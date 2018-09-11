package software.wings.utils;

import static io.harness.eraro.ErrorCode.FILE_DOWNLOAD_FAILED;
import static io.harness.eraro.ErrorCode.INVALID_URL;
import static java.lang.String.format;

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

  /**
   * Instantiates a new bounded input stream.
   *
   * @param inputStream the input stream
   */
  public BoundedInputStream(InputStream inputStream) {
    this(inputStream, -1); // no size limit imposed
  }

  /**
   * Instantiates a new bounded input stream.
   *
   * @param inputStream the input stream
   * @param size        the size
   */
  public BoundedInputStream(InputStream inputStream, long size) {
    this.inputStream = inputStream;
    this.size = size;
  }

  /**
   * Gets the bounded stream for url.
   *
   * @param urlString the url string
   * @param size      the size
   * @return the bounded stream for url
   */
  public static BoundedInputStream getBoundedStreamForUrl(String urlString, long size) {
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
        throw new IOException(format("Inputstream size limit exceeded. Allowed limit %s bytes", size));
      }
    }
    return bytesRead;
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read()
   */
  @Override
  public int read() throws IOException {
    return updateTotalBytesRead(inputStream.read());
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read(byte[])
   */
  @Override
  public int read(byte bytes[]) throws IOException {
    return read(bytes, 0, bytes.length);
  }

  /* (non-Javadoc)
   * @see java.io.InputStream#read(byte[], int, int)
   */
  @Override
  public int read(byte bytes[], int off, int len) throws IOException {
    return updateTotalBytesRead(inputStream.read(bytes, off, len));
  }

  public long getTotalBytesRead() {
    return totalBytesRead;
  }
}
