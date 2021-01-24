package io.harness.delegate.task.gitapi.client.impl;

import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.GITHUB_APP;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.TOKEN;
import static io.harness.logging.CommandExecutionStatus.FAILURE;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.gitapi.GitApiFindPRTaskResponse;
import io.harness.delegate.beans.gitapi.GitApiTaskParams;
import io.harness.delegate.beans.gitapi.GitApiTaskResponse;
import io.harness.delegate.beans.gitapi.GitApiTaskResponse.GitApiTaskResponseBuilder;
import io.harness.delegate.task.gitapi.client.GitApiClient;
import io.harness.exception.InvalidRequestException;
import io.harness.git.GitClientHelper;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GithubApiClient implements GitApiClient {
  private final GithubService githubService;
  private final SecretDecryptionService secretDecryptionService;

  private static final String GITHUB_API_URL = "https://api.github.com/";

  @Override
  public DelegateResponseData findPullRequest(GitApiTaskParams gitApiTaskParams) {
    GitApiTaskResponseBuilder responseBuilder = GitApiTaskResponse.builder();
    ConnectorDetails gitConnector = gitApiTaskParams.getConnectorDetails();
    try {
      if (gitConnector == null
          || !gitConnector.getConnectorConfig().getClass().isAssignableFrom(GithubConnectorDTO.class)) {
        throw new InvalidRequestException(
            format("Invalid Connector %s, Need GithubConfig: ", gitConnector.getIdentifier()));
      }
      GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
      String token = retrieveAuthToken(gitConnector);
      String gitApiURL = getGitApiURL(gitConfigDTO.getUrl());

      String prJson = githubService.findPR(gitApiURL, token, null, gitApiTaskParams.getOwner(),
          gitApiTaskParams.getRepo(), gitApiTaskParams.getPrNumber());
      if (isNotBlank(prJson)) {
        responseBuilder.commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .gitApiResult(GitApiFindPRTaskResponse.builder().prJson(prJson).build());
      } else {
        responseBuilder.commandExecutionStatus(FAILURE).errorMessage("Received blank  pr details");
      }
    } catch (Exception e) {
      log.error(new StringBuilder("failed while fetching PR Details using connector: ")
                    .append(gitConnector.getIdentifier())
                    .toString(),
          e);
      responseBuilder.commandExecutionStatus(FAILURE).errorMessage(e.getMessage());
    }

    return responseBuilder.build();
  }

  private String getGitApiURL(String url) {
    if (GitClientHelper.isGithubSAAS(url)) {
      return GITHUB_API_URL;
    } else {
      String domain = GitClientHelper.getGitSCM(url);
      return "https://" + domain + "/api/v3/";
    }
  }

  private String retrieveAuthToken(ConnectorDetails gitConnector) {
    GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
    if (gitConfigDTO.getApiAccess() == null || gitConfigDTO.getApiAccess().getType() == null) {
      throw new InvalidRequestException(
          format("Failed to retrieve token info for github connector: ", gitConnector.getIdentifier()));
    }

    GithubApiAccessSpecDTO spec = gitConfigDTO.getApiAccess().getSpec();
    GithubApiAccessType apiAccessType = gitConfigDTO.getApiAccess().getType();
    secretDecryptionService.decrypt(spec, gitConnector.getEncryptedDataDetails());
    String token = null;
    if (apiAccessType == TOKEN) {
      token = new String(((GithubTokenSpecDTO) spec).getTokenRef().getDecryptedValue());
    } else if (gitConfigDTO.getApiAccess().getType() == GITHUB_APP) {
      token = fetchTokenUsingGithubAppSpec(gitConfigDTO, (GithubAppSpecDTO) spec);
    } else {
      throw new InvalidRequestException(
          format("Failed while retrieving token. Unsupported access type %s for Github accessType. Use Token Access",
              gitConfigDTO.getApiAccess().getType()));
    }

    return token;
  }

  private String fetchTokenUsingGithubAppSpec(GithubConnectorDTO gitConfigDTO, GithubAppSpecDTO spec) {
    GithubAppSpecDTO githubAppSpecDTO = spec;
    return githubService.getToken(GithubAppConfig.builder()
                                      .installationId(githubAppSpecDTO.getInstallationId())
                                      .appId(githubAppSpecDTO.getApplicationId())
                                      .privateKey(new String(githubAppSpecDTO.getPrivateKeyRef().getDecryptedValue()))
                                      .githubUrl(getGitApiURL(gitConfigDTO.getUrl()))
                                      .build(),
        null);
  }
}
