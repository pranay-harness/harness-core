package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(CDC)
public interface JiraRestClient {
  @GET("project") Call<List<JiraProjectBasicNG>> getProjects();

  @GET("issue/{issueKey}")
  Call<JiraIssueNG> getIssue(@Path("issueKey") String issueKey, @Query("expand") String expand);

  @GET("issue/{issueKey}/transitions")
  Call<JiraIssueTransitionsNG> getIssueTransitions(@Path("issueKey") String issueKey);

  @GET("issue/createmeta")
  Call<JiraIssueCreateMetadataNG> getIssueCreateMetadata(@Query("projectKeys") String projectKeys,
      @Query("issueTypeNames") String issueTypes, @Query("expand") String expand);

  @GET("issue/{issueKey}/editmeta")
  Call<JiraIssueUpdateMetadataNG> getIssueUpdateMetadata(@Path("issueKey") String issueKey);

  @GET("status") Call<List<JiraStatusNG>> getStatuses();

  @GET("project/{projectKey}/statuses")
  Call<List<JiraIssueTypeNG>> getProjectStatuses(@Path("projectKey") String projectKey);

  @POST("issue") Call<JiraIssueNG> createIssue(@Body JiraCreateIssueRequestNG createIssueRequest);

  @PUT("issue/{issueId}")
  Call<Void> updateIssue(@Path("issueKey") String issueKey, @Body JiraUpdateIssueRequestNG updateIssueRequest);
}
