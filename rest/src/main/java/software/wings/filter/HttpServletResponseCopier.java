package software.wings.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * HttpServletResponseCopier based on https://github.com/ukwa/interject/blob/master
 * /interject-servlet-filter/src/main /java/uk/bl/wa/interject/filter/HttpServletResponseCopier.java
 *
 * @author Rishi
 */
public class HttpServletResponseCopier extends HttpServletResponseWrapper {
  private ServletOutputStream outputStream;
  private PrintWriter writer;
  private ServletOutputStreamCopier copier;

  /**
   * Instantiates a new http servlet response copier.
   *
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public HttpServletResponseCopier(HttpServletResponse response) throws IOException {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("getWriter() has already been called on this response.");
    }

    if (outputStream == null) {
      outputStream = getResponse().getOutputStream();
      copier = new ServletOutputStreamCopier(outputStream);
    }

    return copier;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (outputStream != null) {
      throw new IllegalStateException("getOutputStream() has already been called on this response.");
    }

    if (writer == null) {
      copier = new ServletOutputStreamCopier(getResponse().getOutputStream());
      writer = new PrintWriter(new OutputStreamWriter(copier, getResponse().getCharacterEncoding()), true);
    }

    return writer;
  }

  /* (non-Javadoc)
   * @see javax.servlet.ServletResponseWrapper#flushBuffer()
   */
  @Override
  public void flushBuffer() throws IOException {
    try {
      if (writer != null) {
        writer.flush();
      } else if (outputStream != null) {
        copier.flushStream();
        copier.flush();
      }
    } finally {
      if (null != writer) {
        writer.close();
      }
      if (null != copier) {
        copier.close();
      }
    }
  }

  /**
   * Get copy byte [ ].
   *
   * @return the byte [ ]
   */
  public byte[] getCopy() {
    if (copier != null) {
      return copier.getCopy();
    } else {
      return new byte[0];
    }
  }
}
