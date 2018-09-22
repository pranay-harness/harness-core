package software.wings.helpers.ext.nexus;

import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeViewResponse;
import software.wings.helpers.ext.nexus.model.Project;

import java.util.Map;

/**
 * Created by srinivas on 3/30/17.
 */
public interface NexusRestClient {
  /**
   * List project plans call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories")
  Call<RepositoryListResourceResponse> getAllRepositories(@Header("Authorization") String authorization);

  /**
   * List Repository contents call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/content/")
  Call<ContentListResourceResponse> getRepositoryContents(
      @Header("Authorization") String authorization, @Path("repoId") String repoId);

  /**
   * List  Repository Contents .
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/content/{relativePath}")
  Call<ContentListResourceResponse> getRepositoryContents(@Header("Authorization") String authorization,
      @Path("repoId") String repoId, @Path("relativePath") String relativePath);

  /**
   * List Repository contents call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/index_content/")
  Call<IndexBrowserTreeViewResponse> getIndexContent(
      @Header("Authorization") String authorization, @Path("repoId") String repoId);

  /**
   * List Repository contents call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/index_content/{path}/")
  Call<IndexBrowserTreeViewResponse> getIndexContent(
      @Header("Authorization") String authorization, @Path("repoId") String repoId, @Path("path") String path);

  @GET
  Call<IndexBrowserTreeViewResponse> getIndexContentByUrl(
      @Header("Authorization") String authorization, @Url String url);

  @GET
  Call<Project> getPomModel(
      @Header("Authorization") String authorization, @Url String url, @QueryMap Map<String, String> options);
}
