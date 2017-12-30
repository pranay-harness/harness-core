package software.wings.helpers.ext.gcr;

import static java.util.Collections.emptyList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.Switch.unhandled;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GcpHelperService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author brett on 8/2/17
 */
@Singleton
public class GcrServiceImpl implements GcrService {
  private GcpHelperService gcpHelperService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final int CONNECT_TIMEOUT = 5; // TODO:: read from config

  @Inject
  public GcrServiceImpl(GcpHelperService gcpHelperService) {
    this.gcpHelperService = gcpHelperService;
  }

  private GcrRestClient getGcrRestClient(String registryHostName) {
    OkHttpClient okHttpClient =
        new OkHttpClient().newBuilder().connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS).build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(getUrl(registryHostName))
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(GcrRestClient.class);
  }

  private String getUrl(String gcrHostName) {
    return "https://" + gcrHostName + (gcrHostName.endsWith("/") ? "" : "/");
  }

  @Override
  public List<BuildDetails> getBuilds(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds) {
    String imageName = artifactStreamAttributes.getImageName();
    try {
      Response<GcrImageTagResponse> response =
          getGcrRestClient(artifactStreamAttributes.getRegistryHostName())
              .listImageTags(getBasicAuthHeader(gcpConfig, encryptionDetails), imageName)
              .execute();
      checkValidImage(imageName, response);
      return processBuildResponse(response.body());
    } catch (IOException e) {
      logger.error("Error occurred while getting builds from " + imageName, e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, "message", e.getMessage());
    }
  }

  private void checkValidImage(String imageName, Response<GcrImageTagResponse> response) {
    if (response.code() == 404) { // Page not found
      Map<String, Object> params = new HashMap<>();
      params.put("name", imageName);
      params.put("reason", " Reason: Image name does not exist.");
      throw new WingsException(params, ErrorCode.INVALID_ARTIFACT_SOURCE);
    }
  }

  private List<BuildDetails> processBuildResponse(GcrImageTagResponse dockerImageTagResponse) {
    if (dockerImageTagResponse != null && dockerImageTagResponse.getTags() != null) {
      return dockerImageTagResponse.getTags()
          .stream()
          .map(tag -> aBuildDetails().withNumber(tag).build())
          .collect(Collectors.toList());
    }
    return emptyList();
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    return null;
  }

  @Override
  public boolean verifyImageName(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    try {
      String imageName = artifactStreamAttributes.getImageName();
      Response<GcrImageTagResponse> response =
          getGcrRestClient(artifactStreamAttributes.getRegistryHostName())
              .listImageTags(getBasicAuthHeader(gcpConfig, encryptionDetails), imageName)
              .execute();
      if (!isSuccessful(response)) {
        // image not found or user doesn't have permission to list image tags
        logger.warn("Image name [" + imageName + "] does not exist in Google Container Registry.");
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args",
            "Image name [" + imageName + "] does not exist in Google Container Registry.");
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT, "name", "Registry server");
    }
    return true;
  }

  @Override
  public boolean validateCredentials(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    try {
      GcrRestClient registryRestClient = getGcrRestClient(artifactStreamAttributes.getRegistryHostName());
      String basicAuthHeader = getBasicAuthHeader(gcpConfig, encryptionDetails);
      Response response =
          registryRestClient.listImageTags(basicAuthHeader, artifactStreamAttributes.getImageName()).execute();
      return isSuccessful(response);
    } catch (IOException e) {
      logger.error(
          "Error occurred while sending request to server " + artifactStreamAttributes.getRegistryHostName(), e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, "message", e.getMessage());
    }
  }

  private String getBasicAuthHeader(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    GoogleCredential gc = gcpHelperService.getGoogleCredential(gcpConfig, encryptionDetails);

    if (gc.refreshToken()) {
      return Credentials.basic("_token", gc.getAccessToken());
    } else {
      String msg = "Could not refresh token for google cloud provider";
      logger.warn(msg);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, "message", msg);
    }
  }

  private boolean isSuccessful(Response<?> response) throws IOException {
    int code = response.code();
    switch (code) {
      case 404:
      case 400:
        return false;
      case 401:
        throw new WingsException(
            ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Google Container Registry credentials");
      default:
        unhandled(code);
    }
    return true;
  }

  /**
   * The type GCR image tag response.
   */
  public static class GcrImageTagResponse {
    private List<String> child;
    private String name;
    private List<String> tags;
    private Map manifest;

    public Map getManifest() {
      return manifest;
    }

    public void setManifest(Map manifest) {
      this.manifest = manifest;
    }

    public List<String> getChild() {
      return child;
    }

    public void setChild(List<String> child) {
      this.child = child;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets tags.
     *
     * @return the tags
     */
    public List<String> getTags() {
      return tags;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     */
    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }
}
