package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT_BRANCH;

import static javax.ws.rs.Priorities.HEADER_DECORATOR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitEntityInfo.GitEntityInfoKeys;

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
@OwnedBy(DX)
public class GitSyncThreadDecorator implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    final String branchName = getRequestParamFromContext(GitEntityInfoKeys.branch, pathParameters, queryParameters);
    final String filePath = getRequestParamFromContext(GitEntityInfoKeys.filePath, pathParameters, queryParameters);
    final String yamlGitConfigId =
        getRequestParamFromContext(GitEntityInfoKeys.yamlGitConfigId, pathParameters, queryParameters);
    final String accountId = getRequestParamFromContext(GitEntityInfoKeys.accountId, pathParameters, queryParameters);
    // todo(abhinav): see how we can add repo and other details automatically, if not we expect it in every request.
    final GitEntityInfo branchInfo = GitEntityInfo.builder()
                                         .branch(branchName)
                                         .filePath(filePath)
                                         .yamlGitConfigId(yamlGitConfigId)
                                         .accountId(accountId)
                                         .build();
    GitSyncBranchThreadLocal.set(branchInfo);
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : DEFAULT_BRANCH;
  }
}
