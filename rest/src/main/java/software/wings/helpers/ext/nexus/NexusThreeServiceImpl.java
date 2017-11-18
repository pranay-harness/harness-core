package software.wings.helpers.ext.nexus;

import static java.util.Collections.singletonList;

import com.google.inject.Singleton;

import okhttp3.Credentials;
import org.apache.commons.lang3.tuple.Pair;
import retrofit2.Response;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.model.DockerImageResponse;
import software.wings.helpers.ext.nexus.model.RepositoryRequest;
import software.wings.helpers.ext.nexus.model.RepositoryResponse;
import software.wings.helpers.ext.nexus.model.RequestData;
import software.wings.security.encryption.EncryptedDataDetail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 11/16/17.
 */
@Singleton
public class NexusThreeServiceImpl extends AbstractNexusService {
  public static void main(String... args) {
    NexusConfig nexusConfig = NexusConfig.builder()
                                  .nexusUrl("http://ec2-52-91-76-213.compute-1.amazonaws.com:8081")
                                  .username("admin")
                                  .password("admin123".toCharArray())
                                  .build();

    NexusThreeServiceImpl nexus3Service = new NexusThreeServiceImpl();
    nexus3Service.getDockerImages(nexusConfig, null, "docker-group");

    //    ObjectMapper mapperObj = new ObjectMapper();
    //
    //
    //    try {
    //      // get Employee object as a json string
    //      String jsonStr = mapperObj.writeValueAsString(repositoryRequest);
    //      System.out.println(jsonStr);
    //    } catch (IOException e) {
    //      // TODO Auto-generated catch block
    //      e.printStackTrace();
    //    }
  }

  private Map<String, String> getDockerRepositories(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException {
    RepositoryRequest repositoryRequest =
        RepositoryRequest.builder()
            .action("coreui_Repository")
            .method("readReferences")
            .type("rpc")
            .tid(15)
            .data(singletonList(
                RequestData.builder()
                    .filter(singletonList(RequestData.Filter.builder().property("format").value("docker").build()))
                    .build()))
            .build();

    NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
    Response<RepositoryResponse> response =
        nexusThreeRestClient
            .getRepositories(
                Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repositoryRequest)
            .execute();
    if (isSuccessful(response)) {
      if (response.body().getResult().isSuccess()) {
        return response.body().getResult().getData().stream().collect(
            Collectors.toMap(o -> o.getId(), o -> o.getName()));
      } else {
        logger.warn("Failed to fetch the repositories as request is not success");
      }
    }
    return new HashMap<>();
  }

  @Override
  public Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      return getDockerRepositories(nexusConfig, encryptionDetails);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public List<String> getDockerImages(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repository) {
    List<String> images = new ArrayList<>();
    try {
      NexusThreeRestClient nexusThreeRestClient = getNexusThreeClient(nexusConfig, encryptionDetails);
      Response<DockerImageResponse> response =
          nexusThreeRestClient
              .getDockerImages(
                  Credentials.basic(nexusConfig.getUsername(), new String(nexusConfig.getPassword())), repository)
              .execute();
      if (isSuccessful(response)) {
        images = response.body().getRepositories().stream().collect(Collectors.toList());
      } else {
        logger.warn("Failed to fetch the repositories as request is not success");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return images;
  }

  @Override
  public List<String> getArtifactPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) {
    return null;
  }

  @Override
  public List<String> getArtifactPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String name) {
    return null;
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactNames) {
    return null;
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, String groupId, String artifactName,
      String version) {
    return null;
  }

  @Override
  public List<String> getGroupIdPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId) {
    return null;
  }

  @Override
  public List<String> getArtifactNames(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String path) {
    return null;
  }

  @Override
  public List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) {
    return null;
  }

  @Override
  public BuildDetails getLatestVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoId, String groupId, String artifactName) {
    return null;
  }
}
