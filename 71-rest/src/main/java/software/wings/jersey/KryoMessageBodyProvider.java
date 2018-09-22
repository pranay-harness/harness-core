package software.wings.jersey;

import com.google.common.io.ByteStreams;

import software.wings.utils.KryoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * @author peeyushaggarwal
 */
@Provider
@Consumes("application/x-kryo")
@Produces("application/x-kryo")
public class KryoMessageBodyProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
  //
  // MessageBodyWriter
  //

  @Override
  public long getSize(final Object object, final Class<?> type, final Type genericType, final Annotation[] annotations,
      final MediaType mediaType) {
    return -1;
  }

  @Override
  public boolean isWriteable(
      final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return true;
  }

  @Override
  public void writeTo(final Object object, final Class<?> type, final Type genericType, final Annotation[] annotations,
      final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
      throws IOException, WebApplicationException {
    KryoUtils.writeBytes(object, entityStream);
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
  public Object readFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
      final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
      throws IOException, WebApplicationException {
    byte[] bytes = ByteStreams.toByteArray(entityStream);
    return KryoUtils.asObject(bytes);
  }
}
