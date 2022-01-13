/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_ENCODING;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static jodd.util.StringPool.COMMA;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.SupportsEntityTag;

import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@OwnedBy(PL)
@Provider
@Priority(Priorities.USER)
@Singleton
public class EtagFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Context ResourceInfo resourceInfo;
  private static final String GZIP_ENCODING_SUFFIX = "--gzip";
  private static final String GZIP_ENCODING = "gzip";

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String acceptEncoding = requestContext.getHeaderString(ACCEPT_ENCODING);
    List<String> acceptEncodings = emptyList();
    if (isNotEmpty(acceptEncoding)) {
      acceptEncodings = Arrays.asList(acceptEncoding.split(COMMA));
      acceptEncodings = acceptEncodings.stream().map(String::trim).collect(Collectors.toList());
    }

    String ifMatch = requestContext.getHeaderString(IF_MATCH);
    requestContext.getHeaders().replace(IF_MATCH, singletonList(getHeader(ifMatch, acceptEncodings)));

    String ifNoneMatch = requestContext.getHeaderString(IF_NONE_MATCH);
    requestContext.getHeaders().replace(IF_NONE_MATCH, singletonList(getHeader(ifNoneMatch, acceptEncodings)));
  }

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

  private String getHeader(String header, List<String> acceptEncodings) {
    if (header == null) {
      return null;
    }
    if (header.startsWith("W/")) {
      header = header.substring(2).trim();
    }
    if (header.endsWith(GZIP_ENCODING_SUFFIX) && acceptEncodings.contains(GZIP_ENCODING)) {
      header = header.substring(0, header.length() - GZIP_ENCODING_SUFFIX.length());
    }
    return header.trim();
  }
}
