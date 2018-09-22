package software.wings.app;

import com.google.inject.Singleton;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;

@Singleton
public class CharsetResponseFilter implements ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    MediaType type = response.getMediaType();
    if (type != null) {
      if (!type.getParameters().containsKey(MediaType.CHARSET_PARAMETER)) {
        MediaType typeWithCharset = type.withCharset("utf-8");
        response.getHeaders().putSingle("Content-Type", typeWithCharset);
      }
    }
  }
}
