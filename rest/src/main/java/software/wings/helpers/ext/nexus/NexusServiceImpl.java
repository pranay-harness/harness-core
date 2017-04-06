package software.wings.helpers.ext.nexus;

import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Credentials;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
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
import software.wings.helpers.ext.nexus.model.Data;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeNode;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeViewResponse;

/**
 * Created by srinivas on 3/28/17.
 */
@Singleton
public class NexusServiceImpl implements NexusService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public Map<String, String> getRepositories(final NexusConfig nexusConfig) {
    final Map<String, String> repos = new HashMap<>();
    try {
      final Call<RepositoryListResourceResponse> request =
          getRestClient(nexusConfig)
              .getAllRepositories(Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()));
      final Response<RepositoryListResourceResponse> response = request.execute();
      if (isSuccessful(response)) {
        final RepositoryListResourceResponse repositoryResponse = response.body();
        final List<RepositoryListResource> repositories = repositoryResponse.getData();
        repositories.forEach(repo -> { repos.put(repo.getId(), repo.getName()); });
      }
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repositories from Nexus server {} ", nexusConfig.getNexusUrl(), e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
    return repos;
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
  public List<String> getArtifactPaths(NexusConfig nexusConfig, String repoId, String name) {
    try {
      if (name.startsWith("/")) {
        name = name.substring(1);
      }
      final Call<ContentListResourceResponse> request =
          getRestClient(nexusConfig)
              .getRepositoryContents(
                  Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()), repoId, name);
      return getArtifactPaths(request);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository contents  from Nexus server {} for Repository  {} ",
          nexusConfig.getNexusUrl(), repoId, e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(
      NexusConfig nexusConfig, String repoType, String buildNumber, String artifactPathRegex) {
    return null;
  }

  public void getGroupIdPaths(NexusConfig nexusConfig, String repoId, String path, List<String> groupIds) {
    try {
      final String url = getIndexContentPathUrl(nexusConfig, repoId, path);
      final Call<IndexBrowserTreeViewResponse> request =
          getRestClient(nexusConfig)
              .getIndexContentByUrl(
                  Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()), url.toString());
      final Response<IndexBrowserTreeViewResponse> response = request.execute();
      // Check if response successful or not
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("G")) {
            groupIds.add(treeNode.getPath());
            getGroupIdPaths(nexusConfig, repoId, treeNode.getPath(), groupIds);
          } else {
            return;
          }
        }

        );
      }
    } catch (final IOException e) {
      logger.error(
          "Error occurred while retrieving Repository Group Ids  from Nexus server {} for repository under path  {} ",
          nexusConfig.getNexusUrl(), repoId, path, e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
  }

  @Override
  public List<String> getGroupIdPaths(NexusConfig nexusConfig, String repoId) {
    List<String> groupIds = new ArrayList<>();
    try {
      final Call<IndexBrowserTreeViewResponse> request =
          getRestClient(nexusConfig)
              .getIndexContent(Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()), repoId);
      final Response<IndexBrowserTreeViewResponse> response = request.execute();
      // Check if response successful or not
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("G")) {
            groupIds.add(treeNode.getPath());
            getGroupIdPaths(nexusConfig, repoId, treeNode.getPath(), groupIds);
          }
        }

        );
      }
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository Group Ids  from Nexus server {} for Repository  {} ",
          nexusConfig.getNexusUrl(), repoId, e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }

    return groupIds;
  }

  @Override
  public List<String> getArtifactNames(NexusConfig nexusConfig, String repoId, String path) {
    final String url = getIndexContentPathUrl(nexusConfig, repoId, path);
    final List<String> artifactNames = new ArrayList<>();
    try {
      final Call<IndexBrowserTreeViewResponse> request =
          getRestClient(nexusConfig)
              .getIndexContentByUrl(Credentials.basic(nexusConfig.getUsername(), nexusConfig.getPassword()), url);
      final Response<IndexBrowserTreeViewResponse> response = request.execute();
      // Check if response successful or not
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        if (treeNodes != null) {
          treeNodes.forEach(treeNode -> {
            if (treeNode.getType().equals("A")) {
              artifactNames.add(treeNode.getNodeName());
            }
          });
        }
      }
    } catch (final IOException e) {
      logger.error(
          "Error occurred while retrieving artifact names from Nexus server {} for Repository  {} under path {} ",
          nexusConfig.getNexusUrl(), repoId, path, e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
    return artifactNames;
  }

  private String getIndexContentPathUrl(NexusConfig nexusConfig, String repoId, String path) {
    final StringBuilder url = new StringBuilder(getBaseUrl(nexusConfig));
    url.append("service/local/repositories/");
    url.append(repoId).append("/index_content").append(path);
    return url.toString();
  }

  private List<String> getArtifactPaths(Call<ContentListResourceResponse> request) throws IOException {
    final Response<ContentListResourceResponse> response = request.execute();
    final List<String> artifactPaths = new ArrayList<>();
    if (isSuccessful(response)) {
      final ContentListResourceResponse contentResponse = response.body();
      final List<ContentListResource> data = contentResponse.getData();
      data.forEach(artifact -> { artifactPaths.add(artifact.getRelativePath()); });
    }
    return artifactPaths;
  }

  private NexusRestClient getRestClient(final NexusConfig nexusConfig) {
    final String baseUrl = getBaseUrl(nexusConfig);
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
                                  .build();
    NexusRestClient nexusRestClient = retrofit.create(NexusRestClient.class);
    return nexusRestClient;
  }

  private String getBaseUrl(NexusConfig nexusConfig) {
    String baseUrl = nexusConfig.getNexusUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl = baseUrl + "/";
    }
    return baseUrl;
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

  private boolean isSuccessful(Response<?> response) throws IOException {
    if (!response.isSuccessful()) {
      logger.error("Request not successful. Reason: {}", response);

      // TODO : Proper Error handling --> Get the code and map to Wings Error code
      throw new WingsException(getErrorCode(response.code()));
    }
    return true;
  }

  private ErrorCode getErrorCode(int code) {
    switch (code) {
      case 404:
        return ErrorCode.INVALID_REQUEST;
    }

    return ErrorCode.DEFAULT_ERROR_CODE;
  }

  public static void main(String... args) throws Exception {
    NexusConfig nexusConfig = NexusConfig.Builder.aNexusConfig()
                                  .withNexusUrl("http://localhost:8081/nexus/")
                                  .withUsername("admin")
                                  .withPassword("admin123")
                                  .build();

    NexusServiceImpl nexusService = new NexusServiceImpl();
    // nexusService.getRepositories(nexusConfig);
    // nexusService.getGroupIdPaths(nexusConfig, "releases");
    List<String> names = nexusService.getArtifactNames(nexusConfig, "releases", null);
    names.forEach(name -> { System.out.println(name); });
  }
}
