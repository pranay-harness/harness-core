package software.wings.yaml;

import com.google.common.io.ByteStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.utils.KryoUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by bsollish on 8/8/17.
 */
@Provider
@Consumes("text/yaml")
@Produces("text/yaml")
public class AppYamlMessageBodyProvider implements MessageBodyWriter<Application>, MessageBodyReader<Application> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  //
  // MessageBodyWriter
  //

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type == Application.class;
  }

  @Override
  public long getSize(
      Application application, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    // deprecated by JAX-RS 2.0 and ignored by Jersey runtime
    return -1;
  }

  @Override
  public void writeTo(Application application, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream out)
      throws IOException, WebApplicationException {
    logger.info("****************** AppMessageBodyWriter writeTo called");

    Writer writer = new PrintWriter(out);
    writer.write("--- # app.yaml for appId: " + application.getAppId() + "\n");
    writer.write("name: " + application.getName() + "\n");
    writer.write("description: " + application.getDescription() + "\n");

    writer.flush();
    writer.close();
  }

  //
  // MessageBodyReader
  //

  @Override
  public boolean isReadable(
      final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return true;
  }

  @Override
  public Application readFrom(final Class<Application> type, final Type genericType, final Annotation[] annotations,
      final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
      throws IOException, WebApplicationException {
    /* REF from KryoMessageBodyProvider
    byte[] bytes = ByteStreams.toByteArray(entityStream);
    return KryoUtils.asObject(bytes);
    */

    return null;
  }
}