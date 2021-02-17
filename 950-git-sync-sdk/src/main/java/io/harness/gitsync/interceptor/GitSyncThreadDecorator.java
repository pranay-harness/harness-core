package io.harness.gitsync.interceptor;

import static javax.ws.rs.Priorities.HEADER_DECORATOR;

import io.harness.gitsync.interceptor.GitBranchInfo.GitBranchInfoKeys;

import com.google.inject.Singleton;
import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Priority(HEADER_DECORATOR)
@Slf4j
public class GitSyncThreadDecorator implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    final String branchName = getRequestParamFromContext(GitBranchInfoKeys.branch, pathParameters, queryParameters);
    // todo(abhinav): see how we can add repo and other details automatically, if not we expect it in every request.
    final GitBranchInfo branchInfo = GitBranchInfo.builder().branch(branchName).build();
    GitSyncBranchThreadLocal.set(branchInfo);
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }
}
