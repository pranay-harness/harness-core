package software.wings.helpers.ext.nexus;

import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;

/**
 * Created by srinivas on 3/28/17.
 */
@Singleton
public class NexusServiceImpl implements NexusService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public Map<String, String> getRepositories(final NexusConfig nexusConfig) {
    try {
      final Call<RepositoryListResourceResponse> request =
          getRestClient(nexusConfig)
              .getAllRepositories(Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()));
      final Response<RepositoryListResourceResponse> response = request.execute();
      final RepositoryListResourceResponse repositoryResponse = response.body();
      final Map<String, String> repos = new HashMap<>();

      final List<RepositoryListResource> repositories = repositoryResponse.getData();
      repositories.forEach(repo -> { repos.put(repo.getId(), repo.getName()); });
      return repos;
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repositories from Nexus server {} ", nexusConfig.getNexusUrl(), e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
  }

  @Override
  public List<String> getArtifactPaths(NexusConfig nexusConfig, String repoId) {
    try {
      final Call<ContentListResourceResponse> request =
          getRestClient(nexusConfig)
              .getRepositoryContents(Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()), repoId);
      return getArtifactPaths(request);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving repository contents  from Nexus Server {} for repository  {} ",
          nexusConfig.getNexusUrl(), repoId, e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
  }

  @Override
  public List<String> getArtifactPaths(NexusConfig nexusConfig, String repoId, String relativePath) {
    try {
      if (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
      }
      final Call<ContentListResourceResponse> request =
          getRestClient(nexusConfig)
              .getRepositoryContents(
                  Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()), repoId, relativePath);
      return getArtifactPaths(request);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository contents  from Nexus server {} for Repository  {} ",
          nexusConfig.getNexusUrl(), repoId, e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
  }

  private List<String> getArtifactPaths(Call<ContentListResourceResponse> request) throws IOException {
    final Response<ContentListResourceResponse> response = request.execute();
    final ContentListResourceResponse contentResponse = response.body();
    final List<ContentListResource> data = contentResponse.getData();
    final List<String> artifactPaths = new ArrayList<>();
    data.forEach(artifact -> { artifactPaths.add(artifact.getRelativePath()); }

    );
    return artifactPaths;
  }

  private NexusRestClient getRestClient(final NexusConfig nexusConfig) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(nexusConfig.getNexusUrl())
                                  .addConverterFactory(SimpleXmlConverterFactory.create())
                                  .build();
    NexusRestClient nexusRestClient = retrofit.create(NexusRestClient.class);
    return nexusRestClient;
  }

  /**
   * prepareResponseMessage
   */
  private ResponseMessage prepareResponseMessage(final ErrorCode errorCode, final String errorMsg) {
    final ResponseMessage responseMessage = new ResponseMessage();
    responseMessage.setCode(errorCode);
    responseMessage.setErrorType(ResponseTypeEnum.ERROR);
    responseMessage.setMessage(errorMsg);
    return responseMessage;
  }
}
