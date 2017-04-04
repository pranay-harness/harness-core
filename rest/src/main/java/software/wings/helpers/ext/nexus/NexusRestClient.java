package software.wings.helpers.ext.nexus;

import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

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
   * List project plans call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/content/")
  Call<ContentListResourceResponse> getRepositoryContents(
      @Header("Authorization") String authorization, @Path("repoId") String repoId);

  /**
   * List project plans call.
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("service/local/repositories/{repoId}/content/{relativePath}")
  Call<ContentListResourceResponse> getRepositoryContents(@Header("Authorization") String authorization,
      @Path("repoId") String repoId, @Path("relativePath") String relativePath);
}
