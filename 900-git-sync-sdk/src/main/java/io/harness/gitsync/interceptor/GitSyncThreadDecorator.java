package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT_BRANCH;

import static javax.ws.rs.Priorities.HEADER_DECORATOR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
    final String branchName =
        getRequestParamFromContext(GitSyncApiConstants.BRANCH_KEY, pathParameters, queryParameters);
    final String folderPath =
        getRequestParamFromContext(GitSyncApiConstants.FOLDER_PATH, pathParameters, queryParameters);
    final String filePath =
        getRequestParamFromContext(GitSyncApiConstants.FILE_PATH_KEY, pathParameters, queryParameters);
    final String yamlGitConfigId =
        getRequestParamFromContext(GitSyncApiConstants.REPO_IDENTIFIER_KEY, pathParameters, queryParameters);
    final String commitMsg =
        getRequestParamFromContext(GitSyncApiConstants.COMMIT_MSG_KEY, pathParameters, queryParameters);
    final String lastObjectId =
        getRequestParamFromContext(GitSyncApiConstants.LAST_OBJECT_ID_KEY, pathParameters, queryParameters);
    final String createPrKey =
        getRequestParamFromContext(GitSyncApiConstants.CREATE_PR_KEY, pathParameters, queryParameters);
    final String isNewBranch =
        getRequestParamFromContext(GitSyncApiConstants.NEW_BRANCH, pathParameters, queryParameters);
    final String targetBranch =
        getRequestParamFromContext(GitSyncApiConstants.TARGET_BRANCH_FOR_PR, pathParameters, queryParameters);
    // todo(abhinav): see how we can add repo and other details automatically, if not we expect it in every request.
    final GitEntityInfo branchInfo = GitEntityInfo.builder()
                                         .branch(branchName)
                                         .filePath(filePath)
                                         .yamlGitConfigId(yamlGitConfigId)
                                         .commitMsg(commitMsg)
                                         .lastObjectId(lastObjectId)
                                         .folderPath(folderPath)
                                         .createPr(Boolean.valueOf(createPrKey))
                                         .isNewBranch(Boolean.valueOf(isNewBranch))
                                         .targetBranch(targetBranch)
                                         .build();
    GitSyncBranchThreadLocal.set(branchInfo);
  }

  @VisibleForTesting
  String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    try {
      return URLDecoder.decode(queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : DEFAULT_BRANCH,
          Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      log.error("Error in setting request param for {}", key);
    }
    return DEFAULT_BRANCH;
  }
}
