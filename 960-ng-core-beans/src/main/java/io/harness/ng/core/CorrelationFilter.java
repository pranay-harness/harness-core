package io.harness.ng.core;

import com.google.inject.Singleton;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;

@Provider
@Priority(Priorities.USER)
@Singleton
public final class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static UUID generateRandomUuid() {
    final Random rnd = ThreadLocalRandom.current();
    long mostSig = rnd.nextLong();
    long leastSig = rnd.nextLong();

    // Identify this as a version 4 UUID, that is one based on a random value.
    mostSig &= 0xffffffffffff0fffL;
    mostSig |= 0x0000000000004000L;

    // Set the variant identifier as specified for version 4 UUID values.  The two
    // high order bits of the lower word are required to be one and zero, respectively.
    leastSig &= 0x3fffffffffffffffL;
    leastSig |= 0x8000000000000000L;

    return new UUID(mostSig, leastSig);
  }

  @Override
  public void filter(ContainerRequestContext request) {
    String requestId = request.getHeaderString(CorrelationContext.getCorrelationIdKey());
    if (StringUtils.isEmpty(requestId)) {
      requestId = generateRandomUuid().toString();
    }
    CorrelationContext.setCorrelationId(requestId);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    CorrelationContext.clearCorrelationId();
  }
}
