package software.wings.helpers.ext.nexus;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ResponseMessage.Level.ERROR;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getBaseUrl;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getRetrofit;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.handleException;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import okhttp3.Credentials;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeNode;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeViewResponse;
import software.wings.helpers.ext.nexus.model.Project;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

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
import java.util.stream.Collectors;
/**
 * Created by sgurubelli on 11/18/17.
 */
@Singleton
public class NexusTwoServiceImpl {
  private static final Logger logger = LoggerFactory.getLogger(NexusTwoServiceImpl.class);

  @Inject EncryptionService encryptionService;

  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    logger.info("Retrieving repositories");
    final Call<RepositoryListResourceResponse> request =
        getRestClient(nexusConfig, encryptionDetails)
            .getAllRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));

    final Response<RepositoryListResourceResponse> response = request.execute();
    if (isSuccessful(response)) {
      logger.info("Retrieving repositories success");
      return response.body().getData().stream().collect(Collectors.toMap(o -> o.getId(), o -> o.getName()));
    }
    logger.info("No repositories found returning empty map");
    return emptyMap();
  }

  public void getGroupIdPaths(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String path, List<String> groupIds) {
    logger.info("Retrieving groupId paths");
    try {
      Response<IndexBrowserTreeViewResponse> response = getIndexBrowserTreeViewResponseResponse(
          nexusConfig, encryptionDetails, getIndexContentPathUrl(nexusConfig, repoId, path));
      if (isSuccessful(response)) {
        List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("G")) {
            String groupId = treeNode.getPath().replace("/", ".");
            groupIds.add(groupId.substring(1, groupId.length() - 1));
            getGroupIdPaths(nexusConfig, encryptionDetails, repoId, treeNode.getPath(), groupIds);
          } else {
            return;
          }
        });
      }
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository Group Ids from Nexus server " + nexusConfig.getNexusUrl()
              + " for repository " + repoId + " under path " + path,
          e);
      throw new WingsException(
          aResponseMessage().code(INVALID_REQUEST).level(ERROR).message(e.getMessage()).build(), e);
    }
    logger.info("Retrieving groupId paths success");
  }

  public List<String> getGroupIdPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) {
    logger.info("Retrieving groupId paths");
    List<String> groupIds = new ArrayList<>();
    try {
      final Call<IndexBrowserTreeViewResponse> request =
          getRestClient(nexusConfig, encryptionDetails)
              .getIndexContent(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId);
      final Response<IndexBrowserTreeViewResponse> response = request.execute();
      if (isSuccessful(response)) {
        final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
        if (isEmpty(treeNodes)) {
          return groupIds;
        }
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("G")) {
            String groupId = treeNode.getPath().replace("/", ".");
            groupIds.add(groupId.substring(1, groupId.length() - 1));
            getGroupIdPaths(nexusConfig, encryptionDetails, repoId, treeNode.getPath(), groupIds);
          }
        });
      }
    } catch (final IOException e) {
      logger.error("Error occurred while retrieving Repository Group Ids from Nexus server " + nexusConfig.getNexusUrl()
              + " for Repository " + repoId,
          e);
      handleException(e);
    }
    logger.info("Retrieving groupId paths success");
    return groupIds;
  }

  public List<String> getArtifactNames(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String path) throws IOException {
    logger.info("Retrieving Artifact Names");
    final List<String> artifactNames = new ArrayList<>();
    final String url = getIndexContentPathUrl(nexusConfig, repoId, getGroupId(path));
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(nexusConfig, encryptionDetails, url);
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      if (treeNodes != null) {
        treeNodes.forEach((IndexBrowserTreeNode treeNode) -> {
          if (treeNode.getType().equals("A")) {
            artifactNames.add(treeNode.getNodeName());
          }
        });
      }
    }
    logger.info("Retrieving Artifact Names success");
    return artifactNames;
  }

  public List<String> getArtifactPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) throws IOException {
    final Call<ContentListResourceResponse> request =
        getRestClient(nexusConfig, encryptionDetails)
            .getRepositoryContents(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId);
    return getArtifactPaths(request);
  }

  private List<String> getArtifactPaths(Call<ContentListResourceResponse> request) throws IOException {
    final Response<ContentListResourceResponse> response = request.execute();
    final List<String> artifactPaths = new ArrayList<>();
    if (isSuccessful(response)) {
      response.body().getData().forEach(artifact -> artifactPaths.add(artifact.getRelativePath()));
    }
    return artifactPaths;
  }

  public List<String> getArtifactPaths(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String name) throws IOException {
    name = name.charAt(0) == '/' ? name.substring(1) : name;
    final Call<ContentListResourceResponse> request =
        getRestClient(nexusConfig, encryptionDetails)
            .getRepositoryContents(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoId, name);
    return getArtifactPaths(request);
  }

  public List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) throws IOException {
    logger.info("Retrieving versions");
    String url = getIndexContentPathUrl(nexusConfig, repoId, getGroupId(groupId)) + artifactName + "/";
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(nexusConfig, encryptionDetails, url);
    List<BuildDetails> buildDetails = new ArrayList<>();
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      if (treeNodes != null) {
        treeNodes.forEach(treeNode -> {
          if (treeNode.getType().equals("A")) {
            List<IndexBrowserTreeNode> children = treeNode.getChildren();
            for (IndexBrowserTreeNode child : children) {
              if (child.getType().equals("V")) {
                buildDetails.add(
                    aBuildDetails().withNumber(child.getNodeName()).withRevision(child.getNodeName()).build());
              }
            }
          }
        });
      }
    }
    logger.info("Retrieving versions success");
    return buildDetails;
  }

  public BuildDetails getLatestVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) {
    logger.info("Retrieving the latest version for repo {} group {} and artifact {}", repoId, groupId, artifactName);
    Project project = getPomModel(nexusConfig, encryptionDetails, repoId, groupId, artifactName, "LATEST");
    String version = project.getVersion() != null ? project.getVersion() : project.getParent().getVersion();
    logger.info("Retrieving the latest version {}", project);
    return aBuildDetails().withNumber(version).withRevision(version).build();
  }

  public Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactName, String version)
      throws IOException {
    logger.info("Downloading artifact of repo {} group {} artifact {} and version {}", repoType, groupId, artifactName,
        version);
    final Project project = getPomModel(nexusConfig, encryptionDetails, repoType, groupId, artifactName, version);
    final String relativePath = getGroupId(groupId) + project.getArtifactId() + "/"
        + (project.getVersion() != null ? project.getVersion() : project.getParent().getVersion()) + "/";
    final String url = getIndexContentPathUrl(nexusConfig, repoType, relativePath);
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(nexusConfig, encryptionDetails, url);
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      return getUrlInputStream(nexusConfig, encryptionDetails, project, treeNodes, repoType);
    }
    return null;
  }

  private Pair<String, InputStream> getUrlInputStream(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, Project project, List<IndexBrowserTreeNode> treeNodes,
      String repoType) {
    Map<String, String> artifactToUrls = new HashMap<>();
    for (IndexBrowserTreeNode treeNode : treeNodes) {
      for (IndexBrowserTreeNode child : treeNode.getChildren()) {
        if (child.getType().equals("V")) {
          List<IndexBrowserTreeNode> artifacts = child.getChildren();
          if (artifacts != null) {
            for (IndexBrowserTreeNode artifact : artifacts) {
              if (!artifact.getNodeName().endsWith("pom")) {
                artifactToUrls.put(artifact.getNodeName(), artifact.getPath());
                final String resourceUrl = getBaseUrl(nexusConfig) + "service/local/repositories/" + repoType
                    + "/content" + artifact.getPath();
                logger.info("Resource url " + resourceUrl);
                try {
                  encryptionService.decrypt(nexusConfig, encryptionDetails);
                  Authenticator.setDefault(
                      new MyAuthenticator(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
                  return ImmutablePair.of(artifact.getNodeName(), new URL(resourceUrl).openStream());
                } catch (IOException ex) {
                  logger.error("Error occurred while getting the input stream", ex);
                  throw new WingsException(INVALID_REQUEST).addParam("message", ex.getMessage());
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  private Project getPomModel(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoType,
      String groupId, String artifactName, String version) {
    String url = getBaseUrl(nexusConfig) + "service/local/artifact/maven";
    Map<String, String> queryParams = new LinkedHashMap<>();
    queryParams.put("r", repoType);
    queryParams.put("g", groupId);
    queryParams.put("a", artifactName);
    queryParams.put("v", isBlank(version) ? "LATEST" : version);
    Call<Project> request =
        getRestClient(nexusConfig, encryptionDetails)
            .getPomModel(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), url, queryParams);
    try {
      final Response<Project> response = request.execute();
      if (isSuccessful(response)) {
        return response.body();
      } else {
        logger.error("Error while getting the latest version from Nexus url {} and queryParams {}. Reason:{}", url,
            queryParams, response.message());
        throw new WingsException(INVALID_REQUEST).addParam("message", response.message());
      }
    } catch (IOException e) {
      logger.error("Error occurred while retrieving pom model from url " + url, e);
    }
    return new Project();
  }

  private Response<IndexBrowserTreeViewResponse> getIndexBrowserTreeViewResponseResponse(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String url) throws IOException {
    final Call<IndexBrowserTreeViewResponse> request =
        getRestClient(nexusConfig, encryptionDetails)
            .getIndexContentByUrl(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), url);
    return request.execute();
  }

  private String getIndexContentPathUrl(NexusConfig nexusConfig, String repoId, String path) {
    return new StringBuilder(getBaseUrl(nexusConfig))
        .append("service/local/repositories/")
        .append(repoId)
        .append("/index_content")
        .append(path)
        .toString();
  }

  private String getGroupId(String path) {
    return "/" + path.replace(".", "/") + "/";
  }

  private NexusRestClient getRestClient(final NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(nexusConfig, encryptionDetails);
    return getRetrofit(getBaseUrl(nexusConfig), SimpleXmlConverterFactory.createNonStrict())
        .create(NexusRestClient.class);
  }

  static class MyAuthenticator extends Authenticator {
    private String username, password;

    MyAuthenticator(String user, String pass) {
      username = user;
      password = pass;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(username, password.toCharArray());
    }
  }
}
