package software.wings.helpers.ext.nexus;

import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.beans.config.NexusConfig.Builder.aNexusConfig;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Singleton;

import okhttp3.Credentials;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
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
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.model.Data;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeNode;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeViewResponse;
import software.wings.helpers.ext.nexus.model.Project;
import software.wings.utils.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLStreamException;

/**
 * Created by srinivas on 3/28/17.
 */
@Singleton
public class NexusServiceImpl implements NexusService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public Map<String, String> getRepositories(final NexusConfig nexusConfig) {
    try {
      return with().atMost(new Duration(20L, TimeUnit.SECONDS)).until(() -> {
        try {
          logger.info("Retrieving repositories");
          final Map<String, String> repos = new HashMap<>();
          final Call<RepositoryListResourceResponse> request =
              getRestClient(nexusConfig)
                  .getAllRepositories(
                      Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
          final Response<RepositoryListResourceResponse> response = request.execute();
          if (isSuccessful(response)) {
            final RepositoryListResourceResponse repositoryResponse = response.body();
            final List<RepositoryListResource> repositories = repositoryResponse.getData();
            repositories.forEach(repo -> repos.put(repo.getId(), repo.getName()));
          }
          logger.info("Retrieving repositories success");
          return repos;
        } catch (WingsException e) {
          throw e;
        } catch (Exception e) {
          logger.error(
              "Error occurred while retrieving Repositories from Nexus server " + nexusConfig.getNexusUrl(), e);
          if (e.getCause() instanceof XMLStreamException) {
            throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Nexus may not be running");
          }
          throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", e.getMessage());
        }
      }, notNullValue());

    } catch (ConditionTimeoutException e) {
      logger.warn("Nexus server request did not succeed within 20 secs", e);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Nexus server took too long to respond");
    }
  }

  @Override
  public List<String> getArtifactPaths(NexusConfig nexusConfig, String repoId) {
    try {
      final Call<ContentListResourceResponse> request =
          getRestClient(nexusConfig)
              .getRepositoryContents(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId);
      return getArtifactPaths(request);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving repository contents  from Nexus Server " + nexusConfig.getNexusUrl()
              + " for repository " + repoId,
          e);
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
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId, name);
      return getArtifactPaths(request);
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository contents from Nexus server " + nexusConfig.getNexusUrl()
              + " for Repository " + repoId,
          e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(
      NexusConfig nexusConfig, String repoType, String groupId, String artifactName, String version) {
    // First Get the maven
    logger.info(
        "Downloading artifact of repo {} group {} artifact {} and version  ", repoType, groupId, artifactName, version);
    final Project project = getPomModel(nexusConfig, repoType, groupId, artifactName, version);
    final String relativePath = getGroupId(groupId) + project.getArtifactId() + "/"
        + (project.getVersion() != null ? project.getVersion() : project.getParent().getVersion()) + "/";
    final String url = getIndexContentPathUrl(nexusConfig, repoType, relativePath);
    try {
      final Response<IndexBrowserTreeViewResponse> response = getIndexBrowserTreeViewResponseResponse(nexusConfig, url);
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        return getUrlInputStream(nexusConfig, project, treeNodes, repoType);
      }
    } catch (IOException e) {
      logger.error("Error occurred while downloading the artifact", e);
    }
    return null;
  }

  private Pair<String, InputStream> getUrlInputStream(
      NexusConfig nexusConfig, Project project, List<IndexBrowserTreeNode> treeNodes, String repoType) {
    Map<String, String> artifactToUrls = new HashMap<>();
    for (IndexBrowserTreeNode treeNode : treeNodes) {
      List<IndexBrowserTreeNode> children = treeNode.getChildren();
      for (IndexBrowserTreeNode child : children) {
        if (child.getType().equals("V")) {
          List<IndexBrowserTreeNode> artifacts = child.getChildren();
          for (IndexBrowserTreeNode artifact : artifacts) {
            if (!artifact.getNodeName().endsWith("pom")) {
              artifactToUrls.put(artifact.getNodeName(), artifact.getPath());
              final String resourceUrl =
                  getBaseUrl(nexusConfig) + "service/local/repositories/" + repoType + "/content" + artifact.getPath();
              logger.info("Resource url " + resourceUrl);
              try {
                Authenticator.setDefault(
                    new MyAuthenticator(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
                return ImmutablePair.of(artifact.getNodeName(), new URL(resourceUrl).openStream());
              } catch (IOException ex) {
                logger.error("Error occurred while getting the input stream", ex);
                throw new WingsException(ErrorCode.INVALID_REQUEST, "message", ex.getMessage());
              }
            }
          }
        }
      }
    }

    return null;
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(
      NexusConfig nexusConfig, String repoType, String groupId, String artifactName) {
    // First Get the maven pom model
    return downloadArtifact(nexusConfig, repoType, groupId, artifactName, null);
  }

  public void getGroupIdPaths(NexusConfig nexusConfig, String repoId, String path, List<String> groupIds) {
    logger.info("Retrieving groupId paths");
    try {
      final String url = getIndexContentPathUrl(nexusConfig, repoId, path);
      final Response<IndexBrowserTreeViewResponse> response =
          getIndexBrowserTreeViewResponseResponse(nexusConfig, url.toString());
      // Check if response successful or not
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("G")) {
            String groupId = treeNode.getPath();
            groupId = groupId.replace("/", ".");
            groupIds.add(groupId.substring(1, groupId.length() - 1));
            getGroupIdPaths(nexusConfig, repoId, treeNode.getPath(), groupIds);
          } else {
            return;
          }
        }

        );
      }
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository Group Ids from Nexus server " + nexusConfig.getNexusUrl()
              + " for repository " + repoId + " under path " + path,
          e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.INVALID_REQUEST, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
    logger.info("Retrieving groupId paths success");
  }

  @Override
  public List<String> getGroupIdPaths(NexusConfig nexusConfig, String repoId) {
    logger.info("Retrieving groupId paths");
    List<String> groupIds = new ArrayList<>();
    try {
      final Call<IndexBrowserTreeViewResponse> request =
          getRestClient(nexusConfig)
              .getIndexContent(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId);
      final Response<IndexBrowserTreeViewResponse> response = request.execute();
      // Check if response successful or not
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        if (treeNodes == null) {
          return groupIds;
        }
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("G")) {
            String groupId = treeNode.getPath();
            groupId = groupId.replace("/", ".");
            groupIds.add(groupId.substring(1, groupId.length() - 1));
            getGroupIdPaths(nexusConfig, repoId, treeNode.getPath(), groupIds);
          }
        }

        );
      }
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository Group Ids from Nexus server " + nexusConfig.getNexusUrl()
              + " for Repository " + repoId,
          e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
    logger.info("Retrieving groupId paths success");
    return groupIds;
  }

  @Override
  public List<String> getArtifactNames(NexusConfig nexusConfig, String repoId, String path) {
    logger.info("Retrieving Artifact Names");
    final List<String> artifactNames = new ArrayList<>();
    String modifiedPath = getGroupId(path);
    final String url = getIndexContentPathUrl(nexusConfig, repoId, modifiedPath);
    try {
      final Response<IndexBrowserTreeViewResponse> response = getIndexBrowserTreeViewResponseResponse(nexusConfig, url);
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
      logger.error("Error occurred while retrieving artifact names from Nexus server " + nexusConfig.getNexusUrl()
              + " for Repository " + repoId + " under path " + path,
          e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.DEFAULT_ERROR_CODE, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
    logger.info("Retrieving Artifact Names success");
    return artifactNames;
  }

  private String getGroupId(String path) {
    String modifiedPath = path.replace(".", "/");
    modifiedPath = "/" + modifiedPath + "/";
    return modifiedPath;
  }

  @Override
  public List<BuildDetails> getVersions(NexusConfig nexusConfig, String repoId, String groupId, String artifactName) {
    logger.info("Retrieving versions");
    String modifiedPath = getGroupId(groupId);
    String url = getIndexContentPathUrl(nexusConfig, repoId, modifiedPath);
    url = url + artifactName + "/";
    List<BuildDetails> buildDetails = new ArrayList<>();
    try {
      final Response<IndexBrowserTreeViewResponse> response = getIndexBrowserTreeViewResponseResponse(nexusConfig, url);
      // Check if response successful or not
      if (isSuccessful(response)) {
        final IndexBrowserTreeViewResponse treeViewResponse = response.body();
        final Data data = treeViewResponse.getData();
        final List<IndexBrowserTreeNode> treeNodes = data.getChildren();
        if (treeNodes != null) {
          treeNodes.forEach(treeNode -> {
            if (treeNode.getType().equals("A")) {
              List<IndexBrowserTreeNode> children = treeNode.getChildren();
              for (IndexBrowserTreeNode child : children) {
                if (child.getType().equals("V")) {
                  BuildDetails build = new BuildDetails();
                  build.setNumber(child.getNodeName());
                  build.setRevision(child.getNodeName());
                  buildDetails.add(build);
                }
              }
            }
          });
        }
      }
    } catch (final IOException e) {
      logger.error(
          String.format(
              "Error occurred while retrieving versions from Nexus server %s for Repository %s under group id %S and artifact name %s",
              nexusConfig.getNexusUrl(), repoId, groupId, artifactName),
          e);
      List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(ErrorCode.INVALID_REQUEST, e.getMessage()));
      throw new WingsException(responseMessages, e.getMessage(), e);
    }
    logger.info("Retrieving versions success");
    return buildDetails;
  }

  @Override
  public BuildDetails getLatestVersion(NexusConfig nexusConfig, String repoId, String groupId, String artifactName) {
    logger.info("Retrieving the latest version for repo {} group {} and artifact {}", repoId, groupId, artifactName);
    Project project = getPomModel(nexusConfig, repoId, groupId, artifactName, "LATEST");
    String version = project.getVersion() != null ? project.getVersion() : project.getParent().getVersion();
    // final String relativePath = getGroupId(groupId) + project.getArtifactId() + "/" + version + "/";
    logger.info("Retrieving the latest version {}", project);
    return aBuildDetails().withNumber(version).withRevision(version).build();
  }

  private Response<IndexBrowserTreeViewResponse> getIndexBrowserTreeViewResponseResponse(
      NexusConfig nexusConfig, String url) throws IOException {
    final Call<IndexBrowserTreeViewResponse> request =
        getRestClient(nexusConfig)
            .getIndexContentByUrl(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), url);
    return request.execute();
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

  private Project getPomModel(
      NexusConfig nexusConfig, String repoType, String groupId, String artifactName, String version) {
    Project project = null;
    if (StringUtils.isBlank(version)) {
      version = "LATEST";
    }
    String resolveUrl = getBaseUrl(nexusConfig) + "service/local/artifact/maven";
    Map<String, String> queryParams = new LinkedHashMap<>();
    queryParams.put("r", repoType);
    queryParams.put("g", groupId);
    queryParams.put("a", artifactName);
    queryParams.put("v", version);
    Call<Project> request =
        getRestClient(nexusConfig)
            .getPomModel(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                resolveUrl, queryParams);
    try {
      final Response<Project> response = request.execute();
      if (isSuccessful(response)) {
        project = response.body();
      } else {
        logger.error("Error while getting the latest version from Nexus url and queryParams {}. Reason:{}", resolveUrl,
            queryParams, response.message());
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        throw new WingsException(errorCode, "message", response.message());
      }
    } catch (IOException e) {
      logger.error("Error occurred while retrieving pom model from url " + resolveUrl, e);
    }
    return project;
  }

  private NexusRestClient getRestClient(final NexusConfig nexusConfig) {
    final String baseUrl = getBaseUrl(nexusConfig);
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
                                  .client(HttpUtil.getUnsafeOkHttpClient())
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
      int code = response.code();
      ErrorCode errorCode = ErrorCode.DEFAULT_ERROR_CODE;

      switch (code) {
        case 404:
          return false;
        case 401:
          throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Nexus credentials");
      }
      throw new WingsException(errorCode, "message", response.message());
    }
    return true;
  }

  static class MyAuthenticator extends Authenticator {
    private String username, password;

    public MyAuthenticator(String user, String pass) {
      username = user;
      password = pass;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(username, password.toCharArray());
    }
  }

  public static void main(String... args) throws Exception {
    String url = "https://nexus.wings.software/"; //"https://127.0.0.1:8000"; //;
    NexusConfig nexusConfig =
        aNexusConfig().withNexusUrl(url).withUsername("admin").withPassword("wings123ss!".toCharArray()).build();

    NexusServiceImpl nexusService = new NexusServiceImpl();
    nexusService.getRepositories(nexusConfig);
    // nexusService.getGroupIdPaths(nexusConfig, "releases");
    // List<String> names = nexusService.getArtifactNames(nexusConfig, "releases", null);

    /*List<BuildDetails> details = nexusService
        .getVersions(nexusConfig, "releases", "org.apache.maven", "maven-artifact");

    details.forEach(name -> {
      System.out.println(name.getNumber());
    });

    Project project = nexusService
        .getPomModel(nexusConfig, "releases", "org.apache.maven", "maven-artifact", "LATEST");

    System.out.println("Project package type " + project.getPackaging());

    Pair<String, InputStream> map = nexusService
        .downloadArtifact(nexusConfig, "releases", "org.apache.maven", "maven-artifact",
            "LATEST");
    System.out.println("Return inputstream " + map.getValue());*/

    List<String> artifactPaths =
        nexusService.getArtifactPaths(nexusConfig, "releases", "org/apache/maven/maven-artifact/3.0.5/");
    artifactPaths.forEach(s -> System.out.println("s = " + s));
  }
}
