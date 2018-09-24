package software.wings.helpers.ext.gcr;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GcpHelperService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author brett on 8/2/17
 */
@Singleton
public class GcrServiceImpl implements GcrService {
  private GcpHelperService gcpHelperService;

  private static final Logger logger = LoggerFactory.getLogger(GcrServiceImpl.class);

  private static final int CONNECT_TIMEOUT = 5; // TODO:: read from config

  @Inject
  public GcrServiceImpl(GcpHelperService gcpHelperService) {
    this.gcpHelperService = gcpHelperService;
  }

  private GcrRestClient getGcrRestClient(String registryHostName) {
    String url = getUrl(registryHostName);
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
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
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, USER).addParam("message", Misc.getMessage(e));
    }
  }

  private void checkValidImage(String imageName, Response<GcrImageTagResponse> response) {
    if (response.code() == 404) { // Page not found
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
          .addParam("args", "Image name [" + imageName + "] does not exist in Google Container Registry.");
    }
  }

  private List<BuildDetails> processBuildResponse(GcrImageTagResponse dockerImageTagResponse) {
    if (dockerImageTagResponse != null && dockerImageTagResponse.getTags() != null) {
      return dockerImageTagResponse.getTags()
          .stream()
          .map(tag -> aBuildDetails().withNumber(tag).build())
          .collect(toList());
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
        return validateImageName(imageName);
      }
    } catch (IOException e) {
      logger.error(Misc.getMessage(e), e);
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT, USER).addParam("name", "Registry server");
    }
    return true;
  }

  private boolean validateImageName(String imageName) {
    // image not found or user doesn't have permission to list image tags
    logger.warn("Image name [" + imageName + "] does not exist in Google Container Registry.");
    throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
        .addParam("args", "Image name [" + imageName + "] does not exist in Google Container Registry.");
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
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, USER).addParam("message", Misc.getMessage(e));
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
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, USER).addParam("message", msg);
    }
  }

  private boolean isSuccessful(Response<?> response) {
    int code = response.code();
    switch (code) {
      case 200:
        return true;
      case 404:
      case 400:
        logger.info("Response code {} received. Mostly with Image does not exist", code);
        return false;
      case 403:
        logger.info("Response code {} received. User not authorized to access GCR Storage", code);
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
            .addParam("message", "User not authorized to access GCR Storage");
      case 401:
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
            .addParam("message", "Invalid Google Container Registry credentials");
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
