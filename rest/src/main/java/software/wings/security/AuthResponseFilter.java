package software.wings.security;

import com.google.inject.Singleton;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * Created by anubhaw on 4/20/16.
 */
@Singleton
public class AuthResponseFilter implements ContainerResponseFilter {
  /* (non-Javadoc)
   * @see javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.container.ContainerRequestContext,
   * javax.ws.rs.container.ContainerResponseContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    UserThreadLocal.unset(); // clear user object from thread local
  }
}
