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

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import software.wings.beans.config.NexusConfig;
import software.wings.common.AlphanumComparator;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.helpers.ext.artifactory.FolderPath;
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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sgurubelli on 11/18/17.
 */
@Singleton
@Slf4j
public class NexusTwoServiceImpl {
  @Inject private EncryptionService encryptionService;
  @Inject private ExecutorService executorService;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    logger.info("Retrieving repositories");
    final Call<RepositoryListResourceResponse> request;
    if (nexusConfig.hasCredentials()) {
      request =
          getRestClient(nexusConfig, encryptionDetails)
              .getAllRepositories(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
    } else {
      request = getRestClient(nexusConfig, encryptionDetails).getAllRepositories();
    }

    final Response<RepositoryListResourceResponse> response = request.execute();
    if (isSuccessful(response)) {
      logger.info("Retrieving repositories success");
      return response.body().getData().stream().collect(
          toMap(RepositoryListResource::getId, RepositoryListResource::getName));
    }
    logger.info("No repositories found returning empty map");
    return emptyMap();
  }

  public List<String> collectGroupIds(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, List<String> groupIds) throws ExecutionException, InterruptedException {
    NexusRestClient nexusRestClient = getRestClient(nexusConfig, encryptionDetails);
    Queue<Future> futures = new ConcurrentLinkedQueue<>();
    Stack<FolderPath> paths = new Stack<>();
    paths.addAll(getFolderPaths(nexusRestClient, nexusConfig, repoId, ""));
    while (isNotEmpty(paths) || isNotEmpty(futures)) {
      while (isNotEmpty(paths)) {
        FolderPath folderPath = paths.pop();
        String path = folderPath.getPath();
        if (folderPath.isFolder()) {
          traverseInParallel(nexusRestClient, nexusConfig, repoId, path, futures, paths);
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
    Map<String, String> versionToArtifactUrls = new HashMap<>();
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      if (treeNodes != null) {
        for (IndexBrowserTreeNode treeNode : treeNodes) {
          if (treeNode.getType().equals("A")) {
            List<IndexBrowserTreeNode> children = treeNode.getChildren();
            for (IndexBrowserTreeNode child : children) {
              if (child.getType().equals("V")) {
                versions.add(child.getNodeName());
                addArtifactUrl(nexusConfig, versionToArtifactUrls, child);
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
        .map(version
            -> aBuildDetails()
                   .withNumber(version)
                   .withRevision(version)
                   .withBuildUrl(versionToArtifactUrls.get(version))
                   .build())
        .collect(toList());
  }

  private void addArtifactUrl(
      NexusConfig nexusConfig, Map<String, String> versionToArtifactUrls, IndexBrowserTreeNode child) {
    if (child.getChildren() != null) {
      List<IndexBrowserTreeNode> artifacts = child.getChildren();
      if (artifacts != null) {
        for (IndexBrowserTreeNode artifact : artifacts) {
          if (!artifact.getNodeName().endsWith("pom")) {
            versionToArtifactUrls.put(child.getNodeName(), constructArtifactDownloadUrl(nexusConfig, artifact));
            break;
          }
        }
      }
    }
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
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactName, String version,
      String delegateId, String taskId, String accountId, ListNotifyResponseData notifyResponseData)
      throws IOException {
    logger.info("Downloading artifact of repo {} group {} artifact {} and version {}", repoType, groupId, artifactName,
        version);
    final String relativePath = getGroupId(groupId) + artifactName + "/" + version + "/";
    final String url = getIndexContentPathUrl(nexusConfig, repoType, relativePath);
    logger.info("Url {}", url);
    final Response<IndexBrowserTreeViewResponse> response =
        getIndexBrowserTreeViewResponseResponse(getRestClient(nexusConfig, encryptionDetails), nexusConfig, url);
    if (isSuccessful(response)) {
      final List<IndexBrowserTreeNode> treeNodes = response.body().getData().getChildren();
      return getUrlInputStream(
          nexusConfig, encryptionDetails, treeNodes, delegateId, taskId, accountId, notifyResponseData);
    }
    return null;
  }

  private Pair<String, InputStream> getUrlInputStream(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, List<IndexBrowserTreeNode> treeNodes, String delegateId,
      String taskId, String accountId, ListNotifyResponseData res) {
    for (IndexBrowserTreeNode treeNode : treeNodes) {
      for (IndexBrowserTreeNode child : treeNode.getChildren()) {
        if (child.getType().equals("V")) {
          List<IndexBrowserTreeNode> artifacts = child.getChildren();
          if (artifacts != null) {
            for (IndexBrowserTreeNode artifact : artifacts) {
              if (!artifact.getNodeName().endsWith("pom")) {
                String artifactUrl = constructArtifactDownloadUrl(nexusConfig, artifact);
                logger.info("Artifact Url:" + artifactUrl);
                try {
                  if (nexusConfig.hasCredentials()) {
                    encryptionService.decrypt(nexusConfig, encryptionDetails);
                    Authenticator.setDefault(
                        new MyAuthenticator(nexusConfig.getUsername(), new String(nexusConfig.getPassword())));
                  }
                  URL url = new URL(artifactUrl);
                  URLConnection conn = url.openConnection();
                  if (conn instanceof HttpsURLConnection) {
                    HttpsURLConnection conn1 = (HttpsURLConnection) url.openConnection();
                    conn1.setHostnameVerifier((hostname, session) -> true);
                    conn1.setSSLSocketFactory(Http.getSslContext().getSocketFactory());
                    artifactCollectionTaskHelper.addDataToResponse(
                        ImmutablePair.of(artifact.getNodeName(), conn1.getInputStream()), artifactUrl, res, delegateId,
                        taskId, accountId);
                  } else {
                    artifactCollectionTaskHelper.addDataToResponse(
                        ImmutablePair.of(artifact.getNodeName(), conn.getInputStream()), artifactUrl, res, delegateId,
                        taskId, accountId);
                  }

                } catch (IOException ex) {
                  throw new InvalidRequestException(ExceptionUtils.getMessage(ex), ex);
                }
              }
            }
          }
        }
      }
    }
    return null;
  }

  @NotNull
  private String constructArtifactDownloadUrl(NexusConfig nexusConfig, IndexBrowserTreeNode artifact) {
    StringBuilder artifactUrl = new StringBuilder(getBaseUrl(nexusConfig));
    artifactUrl.append("service/local/artifact/maven/content?r=")
        .append(artifact.getRepositoryId())
        .append("&g=")
        .append(artifact.getGroupId())
        .append("&a=")
        .append(artifact.getArtifactId())
        .append("&v=")
        .append(artifact.getVersion());
    if (isNotEmpty(artifact.getPackaging())) {
      artifactUrl.append("&p=").append(artifact.getPackaging());
    }
    if (isNotEmpty(artifact.getExtension())) {
      artifactUrl.append("&e=").append(artifact.getExtension());
    }
    if (isNotEmpty(artifact.getClassifier())) {
      artifactUrl.append("&c=").append(artifact.getClassifier());
    }
    return artifactUrl.toString();
  }

  private Project getPomModel(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoType,
      String groupId, String artifactName, String version) {
    String url = getBaseUrl(nexusConfig) + "service/local/artifact/maven";
    Map<String, String> queryParams = new LinkedHashMap<>();
    queryParams.put("r", repoType);
    queryParams.put("g", groupId);
    queryParams.put("a", artifactName);
    queryParams.put("v", isBlank(version) ? "LATEST" : version);
    Call<Project> request;

    if (nexusConfig.hasCredentials()) {
      request = getRestClient(nexusConfig, encryptionDetails)
                    .getPomModel(Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())),
                        url, queryParams);
    } else {
      request = getRestClient(nexusConfig, encryptionDetails).getPomModel(url, queryParams);
    }
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
    Call<IndexBrowserTreeViewResponse> request;
    if (nexusConfig.hasCredentials()) {
      request = nexusRestClient.getIndexContentByUrl(
          Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), url);
    } else {
      request = nexusRestClient.getIndexContentByUrl(url);
    }
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
        final Call<IndexBrowserTreeViewResponse> request;
        if (nexusConfig.hasCredentials()) {
          request = nexusRestClient.getIndexContent(
              Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repoKey);
        } else {
          request = nexusRestClient.getIndexContent(repoKey);
        }
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
    if (nexusConfig.hasCredentials()) {
      encryptionService.decrypt(nexusConfig, encryptionDetails);
    }
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
