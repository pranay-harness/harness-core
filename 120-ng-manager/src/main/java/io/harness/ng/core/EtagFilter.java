package io.harness.ng.core;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;

import com.google.inject.Singleton;

import io.harness.ng.core.dto.SupportsEntityTag;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
@Singleton
public class EtagFilter implements ContainerResponseFilter {
  @Context ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (responseContext.getEntity() instanceof SupportsEntityTag) {
      SupportsEntityTag response = (SupportsEntityTag) responseContext.getEntity();
      if (response.getEntityTag() != null) {
        responseContext.getHeaders().add(ETAG, "W/" + response.getEntityTag());
        String ifNoneMatch = requestContext.getHeaders().getFirst(IF_NONE_MATCH);

        if (isNotEmpty(ifNoneMatch) && ifNoneMatch.equals(response.getEntityTag())) {
          responseContext.setStatusInfo(NOT_MODIFIED);
          responseContext.setStatus(NOT_MODIFIED.getStatusCode());
        }
      }
    }
  }
}
