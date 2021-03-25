package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.network.SafeHttpCall;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.hibernate.validator.constraints.NotBlank;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Slf4j
public class JiraClient {
  private static final int CONNECT_TIMEOUT = 5;
  private static final int READ_TIMEOUT = 15;

  private final JiraInternalConfig config;
  private final JiraRestClient restClient;

  /**
   * Create a new jira client instance.
   *
   * @param config the url and credentials for the jira instance
   */
  public JiraClient(JiraInternalConfig config) {
    this.config = config;
    this.restClient = createRestClient();
  }

  /**
   * Get all projects for the jira instance.
   *
   * @return the list of projects
   */
  public List<JiraProjectBasicNG> getProjects() {
    return executeCall(restClient.getProjects(), "fetching projects");
  }

  /**
   * Get an issue by issue key.
   *
   * There is special handling for these fields:
   * - project: returns 3 field - "Project Key", "Project Name", "__project" (whole object for internal use)
   * - issuetype: returns 2 fields - "Issue Type", "__issuetype" (whole object for internal use)
   * - status: returns 2 fields - "Status", "__status" (whole object for internal use)
   * - timetracking: returns 2 fields - "Original Estimate", "Remaining Estimate"
   * - Fields treated as OPTION type fields:
   *   - resolution
   *   - component
   *   - priority
   *   - version
   *
   * @param issueKey the issue key
   * @return the issue with the given key
   */
  public JiraIssueNG getIssue(@NotBlank String issueKey) {
    return executeCall(restClient.getIssue(issueKey, "names,schema"), "fetching issue");
  }

  /**
   * Get the issue create metadata information - schema and other information for all project(s), issue type(s) and
   * fields.
   *
   * There is special handling for these fields:
   * - project and issue type are not part of the fields (they are already passed as parameters)
   * - status (if fetchStatus is true): returned  as an OPTION field with possible status values. Is projectKey is not
   *   provided, we fetch all the statuses across the jira instance and add it to all issue types
   * - timetracking: returned as 2 string fields - "Original Estimate", "Remaining Estimate"
   * - Fields treated as OPTION type fields:
   *   - resolution
   *   - component
   *   - priority
   *   - version
   *
   * @param projectKey  the project key - can be null if not known
   * @param issueType   the issue type - can be null if not known
   * @param expand      the expand query parameter - if null a default value of `projects.issuetypes.fields` is used
   * @param fetchStatus should also fetch status
   * @return the issue create meta
   */
  public JiraIssueCreateMetadataNG getIssueCreateMetadata(
      String projectKey, String issueType, String expand, boolean fetchStatus) {
    JiraIssueCreateMetadataNG createMetadata =
        executeCall(restClient.getIssueCreateMetadata(hasNone(projectKey) ? null : projectKey,
                        hasNone(issueType) ? null : issueType, hasNone(expand) ? "projects.issuetypes.fields" : expand),
            "fetching create meta");

    if (fetchStatus) {
      if (hasNone(projectKey)) {
        // If project key is not present, we get all possible statuses for the jira instance and add them to each issue
        // type. This means in the ui dropdown we might show statuses which at runtime will fail because they are not
        // part of the runtime project and issue type.
        List<JiraStatusNG> statuses = getStatuses();
        createMetadata.updateStatuses(statuses);
      } else {
        List<JiraIssueTypeNG> projectStatuses = getProjectStatuses(projectKey);
        createMetadata.updateProjectStatuses(projectKey, projectStatuses);
      }
    } else {
      // After deserialization to JiraIssueCreateMetadataNG, by default, we receive a status field with no allowed
      // values in all issue types. If fetchStatus is false, remove that field from issue types so that ui doesn't show
      // it as one the fields.
      createMetadata.removeField(JiraConstantsNG.STATUS_NAME);
    }

    return createMetadata;
  }

  private List<JiraStatusNG> getStatuses() {
    return executeCall(restClient.getStatuses(), "fetching statuses");
  }

  private List<JiraIssueTypeNG> getProjectStatuses(@NotBlank String projectKey) {
    return executeCall(restClient.getProjectStatuses(projectKey), "fetching project statuses");
  }

  /**
   * Create an issue in the given project and issue type.
   *
   * There is special handling for these fields:
   * - project: sent as { key: [projectKey] }
   * - issuetype: sent as { id: [issueTypeId] }
   * - status is not part of the fields sent to jira (default status is assigned)
   * - timetracking: sent as { originalEstimate: [originalEstimate], remainingEstimate: [remainingEstimate] }
   * - OPTION type fields are sent as { id: [optionId], name: [optionName], value: [optionValue] } - null fields are
   *   ignored
   * - Fields treated as OPTION type fields:
   *   - resolution
   *   - component
   *   - priority
   *   - version
   *
   * @param projectKey    the project key
   * @param issueTypeName the issue type
   * @param fields        the issue fields - list/array type are represented as comma separated strings - if an array
   *                      element has comma itself, it should be wrapped in quotes
   * @return the created issue
   */
  public JiraIssueNG createIssue(
      @NotBlank String projectKey, @NotBlank String issueTypeName, Map<String, String> fields) {
    JiraIssueCreateMetadataNG createMetaResponse = getIssueCreateMetadata(projectKey, issueTypeName, null, false);
    JiraProjectNG project = createMetaResponse.getProjects().get(projectKey);
    if (project == null) {
      throw new InvalidRequestException(String.format("Invalid project: %s", projectKey));
    }

    JiraIssueTypeNG issueType = project.getIssueTypes().get(issueTypeName);
    if (issueType == null) {
      throw new InvalidRequestException(
          String.format("Invalid issue type in project %s: %s", projectKey, issueTypeName));
    }

    JiraCreateIssueRequestNG createIssueRequest = new JiraCreateIssueRequestNG(project, issueType, fields);
    JiraIssueNG issue = executeCall(restClient.createIssue(createIssueRequest), "creating issue");
    return getIssue(issue.getKey());
  }

  private <T> T executeCall(Call<T> call, String action) {
    try {
      log.info("Sending request for: {}", action);
      T resp = SafeHttpCall.executeWithExceptions(call);
      log.info("Response received from: {}", action);
      return resp;
    } catch (IOException ex) {
      throw new GeneralException(String.format("Error %s at url [%s]", action, config.getJiraUrl()), ex);
    }
  }

  private JiraRestClient createRestClient() {
    String url = config.getJiraUrl() + "rest/api/2/";
    OkHttpClient okHttpClient =
        getOkHttpClientBuilder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .proxy(Http.checkAndGetNonProxyIfApplicable(url))
            .addInterceptor(chain -> {
              Request newRequest =
                  chain.request()
                      .newBuilder()
                      .addHeader("Authorization", Credentials.basic(config.getUsername(), config.getPassword()))
                      .build();
              return chain.proceed(newRequest);
            })
            .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(JiraRestClient.class);
  }
}
