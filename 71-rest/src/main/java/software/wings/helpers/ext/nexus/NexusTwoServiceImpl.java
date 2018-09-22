package software.wings.helpers.ext.nexus;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.quietSleep;
import static java.time.Duration.ofMillis;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getBaseUrl;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.getRetrofit;
import static software.wings.helpers.ext.nexus.NexusServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import okhttp3.Credentials;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import software.wings.beans.config.NexusConfig;
import software.wings.common.AlphanumComparator;
import software.wings.helpers.ext.artifactory.FolderPath;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeNode;
import software.wings.helpers.ext.nexus.model.IndexBrowserTreeViewResponse;
import software.wings.helpers.ext.nexus.model.Project;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
/**
 * Created by sgurubelli on 11/18/17.
 */
@Singleton
public class NexusTwoServiceImpl {
  private static final Logger logger = LoggerFactory.getLogger(NexusTwoServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  @Inject private ExecutorService executorService;

  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    logger.info("Retrieving repositories");
    final Call<RepositoryListResourceResponse> request =
        getRestClient(nexusConfig, encryptionDetails)
            .getAllRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));

    final Response<RepositoryListResourceResponse> response = request.execute();
    if (isSuccessful(response)) {
      logger.info("Retrieving repositories success");
      return response.body().getData().stream().collect(
          toMap(RepositoryListResource::getId, RepositoryListResource::getName));
    }
    logger.info("No repositories found returning empty map");
    return emptyMap();
  }

  public List<String> getGroupIdPaths(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId) throws ExecutionException, InterruptedException {
    return getGroupIdPathsAsync(nexusConfig, encryptionDetails, repoId);
  }

  private List<String> getGroupIdPathsAsync(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoKey) throws ExecutionException, InterruptedException {
    List<String> groupIds = new ArrayList<>();
    NexusRestClient nexusRestClient = getRestClient(nexusConfig, encryptionDetails);
    Queue<Future> futures = new ConcurrentLinkedQueue<>();
    Stack<FolderPath> paths = new Stack<>();
    paths.addAll(getFolderPaths(nexusRestClient, nexusConfig, repoKey, ""));
    while (isNotEmpty(paths) || isNotEmpty(futures)) {
      while (isNotEmpty(paths)) {
        FolderPath folderPath = paths.pop();
        String path = folderPath.getPath();
        if (folderPath.isFolder()) {
          traverseInParallel(nexusRestClient, nexusConfig, repoKey, path, futures, paths);
        } else {
          // Strip out the version
          String[] pathElems = folderPath.getPath().substring(1).split("/");
          if (pathElems.length >= 1) {
            groupIds.add(getGroupId(Arrays.stream(pathElems).limit(pathElems.length - 1).collect(toList())));
          }
        }
      }
      while (!futures.isEmpty() && futures.peek().isDone()) {
        futures.poll().get();
      }
      quietSleep(ofMillis(20)); // avoid busy wait
    }
    return groupIds;
  }

  private void traverseInParallel(NexusRestClient nexusRestClient, NexusConfig nexusConfig, String repoKey, String path,
      Queue<Future> futures, Stack<FolderPath> paths) {
    futures.add(
        executorService.submit(() -> paths.addAll(getFolderPaths(nexusRestClient, nexusConfig, repoKey, path))));
  }

  public List<String> getArtifactNames(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String path) throws IOException {
    logger.info("Retrieving Artifact Names");
    final List<String> artifactNames = new ArrayList<>();
    final String url = getIndexContentPathUrl(nexusConfig, repoId, getGroupId(path));
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(getRestClient(nexusConfig, encryptionDetails), nexusConfig, url);
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
    logger.info("Retrieving versions for repoId {} groupId {} and artifactName {}", repoId, groupId, artifactName);
    String url = getIndexContentPathUrl(nexusConfig, repoId, getGroupId(groupId)) + artifactName + "/";
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(getRestClient(nexusConfig, encryptionDetails), nexusConfig, url);
    List<String> versions = new ArrayList<>();
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      if (treeNodes != null) {
        for (IndexBrowserTreeNode treeNode : treeNodes) {
          if (treeNode.getType().equals("A")) {
            List<IndexBrowserTreeNode> children = treeNode.getChildren();
            for (IndexBrowserTreeNode child : children) {
              if (child.getType().equals("V")) {
                versions.add(child.getNodeName());
              }
            }
          }
        }
      }
    }
    logger.info("Versions come from nexus server {}", versions);
    versions = versions.stream().sorted(new AlphanumComparator()).collect(toList());
    logger.info("After sorting alphanumerically versions {}", versions);
    return versions.stream()
        .map(version -> aBuildDetails().withNumber(version).withRevision(version).build())
        .collect(toList());
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
    logger.info("Url {}", url);
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(getRestClient(nexusConfig, encryptionDetails), nexusConfig, url);
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      return getUrlInputStream(nexusConfig, encryptionDetails, project, treeNodes, repoType);
    }
    return null;
  }

  private Pair<String, InputStream> getUrlInputStream(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, Project project, List<IndexBrowserTreeNode> treeNodes,
      String repoType) {
    for (IndexBrowserTreeNode treeNode : treeNodes) {
      for (IndexBrowserTreeNode child : treeNode.getChildren()) {
        if (child.getType().equals("V")) {
          List<IndexBrowserTreeNode> artifacts = child.getChildren();
          if (artifacts != null) {
            for (IndexBrowserTreeNode artifact : artifacts) {
              if (!artifact.getNodeName().endsWith("pom")) {
                final String resourceUrl = getBaseUrl(nexusConfig) + "service/local/repositories/" + repoType
                    + "/content" + artifact.getPath();
                logger.info("Resource url " + resourceUrl);
                try {
                  encryptionService.decrypt(nexusConfig, encryptionDetails);
                  Authenticator.setDefault(
                      new MyAuthenticator(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
                  return ImmutablePair.of(artifact.getNodeName(), new URL(resourceUrl).openStream());
                } catch (IOException ex) {
                  throw new InvalidRequestException(Misc.getMessage(ex), ex);
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
        throw new InvalidRequestException(response.message());
      }
    } catch (IOException e) {
      logger.error("Error occurred while retrieving pom model from url " + url, e);
    }
    return new Project();
  }

  private Response<IndexBrowserTreeViewResponse> getIndexBrowserTreeViewResponseResponse(
      NexusRestClient nexusRestClient, NexusConfig nexusConfig, String url) throws IOException {
    final Call<IndexBrowserTreeViewResponse> request = nexusRestClient.getIndexContentByUrl(
        Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), url);
    return request.execute();
  }

  private String getIndexContentPathUrl(NexusConfig nexusConfig, String repoId, String path) {
    return getBaseUrl(nexusConfig) + "service/local/repositories/" + repoId + "/index_content" + path;
  }

  private List<FolderPath> getFolderPaths(
      NexusRestClient nexusRestClient, NexusConfig nexusConfig, String repoKey, String repoPath) {
    // Add first level paths
    List<FolderPath> folderPaths = new ArrayList<>();
    try {
      Response<IndexBrowserTreeViewResponse> response;
      if (isEmpty(repoPath)) {
        final Call<IndexBrowserTreeViewResponse> request = nexusRestClient.getIndexContent(
            Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoKey);
        response = request.execute();
      } else {
        response = getIndexBrowserTreeViewResponseResponse(
            nexusRestClient, nexusConfig, getIndexContentPathUrl(nexusConfig, repoKey, repoPath));
      }
      if (isSuccessful(response)) {
        List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
        if (treeNodes != null) {
          treeNodes.forEach(treeNode -> {
            if (treeNode.getType().equals("G")) {
              folderPaths.add(FolderPath.builder().repo(repoKey).path(treeNode.getPath()).folder(true).build());
            } else {
              folderPaths.add(FolderPath.builder().repo(repoKey).path(treeNode.getPath()).folder(false).build());
            }
          });
        }
      }
    } catch (final IOException e) {
      throw new InvalidRequestException("Error occurred while retrieving Repository Group Ids from Nexus server "
              + nexusConfig.getNexusUrl() + " for repository " + repoKey + " under path " + repoPath,
          e);
    }
    return folderPaths;
  }

  private String getGroupId(String path) {
    return "/" + path.replace(".", "/") + "/";
  }

  private String getGroupId(List<String> pathElems) {
    StringBuilder groupIdBuilder = new StringBuilder();
    for (int i = 0; i < pathElems.size(); i++) {
      groupIdBuilder.append(pathElems.get(i));
      if (i != pathElems.size() - 1) {
        groupIdBuilder.append('.');
      }
    }
    return groupIdBuilder.toString();
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
