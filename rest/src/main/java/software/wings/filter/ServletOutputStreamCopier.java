package software.wings.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * HttpServletResponseCopier that creates a copy of the http response payload. Based on
 * https://github.com/ukwa/interject/blob/master/interject-servlet-filter
 * /src/main/java/uk/bl/wa/interject/filter/ServletOutputStreamCopier.java
 *
 * @author Rishi
 */
public class ServletOutputStreamCopier extends ServletOutputStream {
  private static final Logger logger = LoggerFactory.getLogger(ServletOutputStreamCopier.class);
  private OutputStream outputStream;
  private ByteArrayOutputStream copy;

  /**
   * Instantiates a new servlet output stream copier.
   *
   * @param outputStream the output stream
   */
  public ServletOutputStreamCopier(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.copy = new ByteArrayOutputStream(1024);
  }

  /* (non-Javadoc)
   * @see java.io.OutputStream#write(int)
   */
  @Override
  public void write(int b) throws IOException {
    try {
      outputStream.write(b);
      copy.write(b);
    } catch (IOException ex) {
      logger.error("", ex);
    }
  }

  /**
   * Get copy byte [ ].
   *
   * @return the byte [ ]
   */
  public byte[] getCopy() {
    return copy.toByteArray();
  }

  /**
   * Flush stream.
   */
  public void flushStream() {
    try {
      if (null != copy) {
        copy.close();
      }
    } catch (IOException io) {
      logger.error("", io);
    }
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(WriteListener arg0) {}
}
