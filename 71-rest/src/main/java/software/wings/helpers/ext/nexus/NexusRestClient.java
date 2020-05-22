package software.wings.helpers.ext.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.annotations.dev.OwnedBy;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeViewResponse;
import software.wings.helpers.ext.nexus.model.Project;

import java.util.Map;

/**
 * Created by srinivas on 3/30/17.
 */
@OwnedBy(CDC)
public interface NexusRestClient {
  /**
   * List project plans call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories")
  Call<RepositoryListResourceResponse> getAllRepositories(@Header("Authorization") String authorization);

  @GET("service/local/repositories") Call<RepositoryListResourceResponse> getAllRepositories();

  /**
   * List Repository contents call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/content/")
  Call<ContentListResourceResponse> getRepositoryContents(
      @Header("Authorization") String authorization, @Path("repoId") String repoId);

  @GET("service/local/repositories/{repoId}/content/")
  Call<ContentListResourceResponse> getRepositoryContents(@Path("repoId") String repoId);

  /**
   * List  Repository Contents .
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/content/{relativePath}")
  Call<ContentListResourceResponse> getRepositoryContents(@Header("Authorization") String authorization,
      @Path("repoId") String repoId, @Path("relativePath") String relativePath);

  @GET("service/local/repositories/{repoId}/content/{relativePath}")
  Call<ContentListResourceResponse> getRepositoryContentsWithoutCredentials(
      @Path("repoId") String repoId, @Path("relativePath") String relativePath);

  /**
   * List  Repository Contents .
   *
   * @param authorization the authorization
   * @return the call
   */
  @Headers("Accept: application/json")
  @GET("content/repositories/{repoId}/{packageName}")
  Call<JsonNode> getVersions(@Header("Authorization") String authorization, @Path("repoId") String repoId,
      @Path("packageName") String packageName);

  @Headers("Accept: application/json")
  @GET("content/repositories/{repoId}/{packageName}")
  Call<JsonNode> getVersionsWithoutCredentials(@Path("repoId") String repoId, @Path("packageName") String packageName);

  @Headers("Accept: application/json")
  @GET("content/repositories/{repoId}/{packageName}/{version}")
  Call<JsonNode> getVersion(@Header("Authorization") String authorization, @Path("repoId") String repoId,
      @Path("packageName") String packageName, @Path("version") String version);

  @Headers("Accept: application/json")
  @GET("content/repositories/{repoId}/{packageName}/{version}")
  Call<JsonNode> getVersionWithoutCredentials(
      @Path("repoId") String repoId, @Path("packageName") String packageName, @Path("version") String version);

  /**
   * List Repository contents call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/index_content/")
  Call<IndexBrowserTreeViewResponse> getIndexContent(
      @Header("Authorization") String authorization, @Path("repoId") String repoId);

  @GET("service/local/repositories/{repoId}/index_content/")
  Call<IndexBrowserTreeViewResponse> getIndexContent(@Path("repoId") String repoId);

  @GET
  Call<IndexBrowserTreeViewResponse> getIndexContentByUrl(
      @Header("Authorization") String authorization, @Url String url);

  @GET Call<IndexBrowserTreeViewResponse> getIndexContentByUrl(@Url String url);

  @GET
  Call<Project> getPomModel(
      @Header("Authorization") String authorization, @Url String url, @QueryMap Map<String, String> options);

  @GET Call<Project> getPomModel(@Url String url, @QueryMap Map<String, String> options);
}
