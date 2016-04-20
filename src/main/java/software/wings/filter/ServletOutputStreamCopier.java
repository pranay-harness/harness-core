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
  private static Logger logger = LoggerFactory.getLogger(ServletOutputStreamCopier.class);
  private OutputStream outputStream;
  private ByteArrayOutputStream copy;

  public ServletOutputStreamCopier(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.copy = new ByteArrayOutputStream(1024);
  }

  @Override
  public void write(int b) throws IOException {
    try {
      outputStream.write(b);
      copy.write(b);
    } catch (IOException e) {
    }
  }

  public byte[] getCopy() {
    return copy.toByteArray();
  }

  public void flushStream() {
    try {
    } finally {
      try {
        if (null != copy) {
          copy.close();
        }
      } catch (IOException io) {
        io.printStackTrace();
      }
    }
  }

  @Override
  public boolean isReady() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setWriteListener(WriteListener arg0) {
    // TODO Auto-generated method stub
  }
}
