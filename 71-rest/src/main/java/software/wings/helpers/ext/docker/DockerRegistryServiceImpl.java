package software.wings.helpers.ext.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by anubhaw on 1/6/17.
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
public class DockerRegistryServiceImpl implements DockerRegistryService {
  public static final String BEARER = "Bearer ";
  @Inject private EncryptionService encryptionService;
  @Inject private DockerPublicRegistryProcessor dockerPublicRegistryProcessor;
  @Inject private DockerRegistryUtils dockerRegistryUtils;
  private static final String AUTHENTICATE_HEADER = "Www-Authenticate";

  private ExpiringMap<String, String> cachedBearerTokens = ExpiringMap.builder().variableExpiration().build();

  private DockerRegistryRestClient getDockerRegistryRestClient(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(dockerConfig, encryptionDetails);
    OkHttpClient okHttpClient = Http.getUnsafeOkHttpClient(dockerConfig.getDockerRegistryUrl());
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(dockerConfig.getDockerRegistryUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(DockerRegistryRestClient.class);
  }

  @Override
  public List<BuildDetails> getBuilds(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails;
    try {
      if (dockerConfig.hasCredentials()) {
        buildDetails = getBuildDetails(dockerConfig, encryptionDetails, imageName);
      } else {
        buildDetails =
            dockerPublicRegistryProcessor.getBuilds(dockerConfig, encryptionDetails, imageName, maxNumberOfBuilds);
      }
    } catch (Exception e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, WingsException.USER);
    }
    return buildDetails;
  }

  private List<BuildDetails> getBuildDetails(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) throws IOException {
    DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(dockerConfig, encryptionDetails);
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
    List<BuildDetails> buildDetails = new ArrayList<>();
    String token = null;
    Response<DockerImageTagResponse> response = registryRestClient.listImageTags(basicAuthHeader, imageName).execute();
    if (response.code() == 401) { // unauthorized
      token = getToken(dockerConfig, encryptionDetails, response.headers(), registryRestClient);
      response = registryRestClient.listImageTags(BEARER + token, imageName).execute();
      if (response.code() == 401) {
        throw new InvalidCredentialsException("Invalid Credentials while fetching build details", USER);
      }
    }

    if (!isSuccessful(response)) {
      throw new InvalidArtifactServerException(response.message(), USER);
    }

    DockerImageTagResponse dockerImageTagResponse = response.body();
    if (dockerImageTagResponse == null || isEmpty(dockerImageTagResponse.getTags())) {
      logger.warn("There are no tags available for the imageName {}", imageName);
      return buildDetails;
    }
    buildDetails.addAll(processBuildResponse(dockerImageTagResponse, dockerConfig, imageName));
    // TODO: Limit the no of tags
    String baseUrl = response.raw().request().url().toString();
    while (true) {
      String nextLink = findNextLink(response.headers());
      if (nextLink == null) {
        return buildDetails;
      } else {
        logger.info(
            "Using pagination to fetch all the builds. The no of builds fetched so far {}", buildDetails.size());
      }
      int queryParamIndex = nextLink.indexOf('?');
      String nextPageUrl =
          queryParamIndex == -1 ? baseUrl.concat(nextLink) : baseUrl.concat(nextLink.substring(queryParamIndex));
      response = registryRestClient.listImageTagsByUrl(BEARER + token, nextPageUrl).execute();
      if (response.code() == 401) { // unauthorized
        token = getToken(dockerConfig, encryptionDetails, response.headers(), registryRestClient);
        response = registryRestClient.listImageTagsByUrl(BEARER + token, nextPageUrl).execute();
      }
      dockerImageTagResponse = response.body();
      if (dockerImageTagResponse == null || isEmpty(dockerImageTagResponse.getTags())) {
        logger.info("There are no more tags available for the imageName {}. Returning tags", imageName);
        return buildDetails;
      }
      buildDetails.addAll(processBuildResponse(dockerImageTagResponse, dockerConfig, imageName));
      if (buildDetails.size() > MAX_NO_OF_TAGS_PER_IMAGE) {
        logger.warn(
            "Image name {} has more than {} tags. We might miss some new tags", imageName, MAX_NO_OF_TAGS_PER_IMAGE);
        break;
      }
    }
    return buildDetails;
  }

  private List<BuildDetails> processBuildResponse(
      DockerImageTagResponse dockerImageTagResponse, DockerConfig dockerConfig, String imageName) {
    String tagUrl = dockerConfig.getDockerRegistryUrl().endsWith("/")
        ? dockerConfig.getDockerRegistryUrl() + imageName + "/tags/"
        : dockerConfig.getDockerRegistryUrl() + "/" + imageName + "/tags/";

    String domainName = Http.getDomainWithPort(dockerConfig.getDockerRegistryUrl());

    return dockerImageTagResponse.getTags()
        .stream()
        .map(tag -> {
          Map<String, String> metadata = new HashMap();
          metadata.put(ArtifactMetadataKeys.image, domainName + "/" + imageName + ":" + tag);
          metadata.put(ArtifactMetadataKeys.tag, tag);
          return aBuildDetails()
              .withNumber(tag)
              .withBuildUrl(tagUrl + tag)
              .withMetadata(metadata)
              .withUiDisplayName("Tag# " + tag)
              .build();
        })
        .collect(toList());
  }

  @Override
  public List<Map<String, String>> getLabels(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, List<String> buildNos) {
    if (!dockerConfig.hasCredentials()) {
      return dockerPublicRegistryProcessor.getLabels(dockerConfig, encryptionDetails, imageName, buildNos);
    }

    DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(dockerConfig, encryptionDetails);
    String authHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
    Function<Headers, String> getToken =
        headers -> getToken(dockerConfig, encryptionDetails, headers, registryRestClient);
    return dockerRegistryUtils.getLabels(registryRestClient, getToken, authHeader, imageName, buildNos);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    return null;
  }

  @Override
  public boolean verifyImageName(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    if (dockerConfig.hasCredentials()) {
      return checkImageName(dockerConfig, encryptionDetails, imageName);
    }
    return dockerPublicRegistryProcessor.verifyImageName(dockerConfig, encryptionDetails, imageName);
  }

  private boolean checkImageName(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    try {
      DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(dockerConfig, encryptionDetails);
      String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
      Response<DockerImageTagResponse> response =
          registryRestClient.listImageTags(basicAuthHeader, imageName).execute();
      if (response.code() == 401) { // unauthorized
        String token = getToken(dockerConfig, encryptionDetails, response.headers(), registryRestClient);
        response = registryRestClient.listImageTags(BEARER + token, imageName).execute();
      }
      if (!isSuccessful(response)) {
        // Image not found or user doesn't have permission to list image tags.
        throw new InvalidArgumentsException(
            ImmutablePair.of("code", "Image name [" + imageName + "] does not exist in Docker registry."), null, USER);
      }
    } catch (IOException e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, USER);
    }
    return true;
  }

  @Override
  public boolean validateCredentials(DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    if (dockerConfig.hasCredentials()) {
      if (isEmpty(dockerConfig.getPassword()) && isEmpty(dockerConfig.getEncryptedPassword())) {
        throw new InvalidArtifactServerException("Password is a required field along with Username", USER);
      }
      DockerRegistryRestClient registryRestClient = null;
      String basicAuthHeader;
      String authHeaderValue;
      Response response;
      DockerRegistryToken dockerRegistryToken;
      try {
        registryRestClient = getDockerRegistryRestClient(dockerConfig, encryptionDetails);
        basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
        response = registryRestClient.getApiVersion(basicAuthHeader).execute();
        if (response.code() == 401) { // unauthorized
          authHeaderValue = response.headers().get(AUTHENTICATE_HEADER);
          dockerRegistryToken = fetchToken(registryRestClient, basicAuthHeader, authHeaderValue);
          if (dockerRegistryToken != null) {
            response = registryRestClient.getApiVersion(BEARER + dockerRegistryToken.getToken()).execute();
          }
        }
        return isSuccessful(response);
      } catch (IOException e) {
        logger.warn("Failed to fetch apiversion with credentials" + e);
        try {
          // This is special case for repositories that require "/v2/" path for getting API version . Eg. Harbor docker
          // registry We get an IO exception with '/v2' path so we are retrying with forward slash API
          basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
          response = registryRestClient.getApiVersionEndingWithForwardSlash(basicAuthHeader).execute();
          if (response.code() == 401) { // unauthorized
            authHeaderValue = response.headers().get(AUTHENTICATE_HEADER);
            dockerRegistryToken = fetchToken(registryRestClient, basicAuthHeader, authHeaderValue);
            if (dockerRegistryToken != null) {
              response = registryRestClient.getApiVersionEndingWithForwardSlash(BEARER + dockerRegistryToken.getToken())
                             .execute();
            }
          }
          return isSuccessful(response);
        } catch (IOException ioException) {
          Exception exception = new Exception(ioException);
          throw new InvalidArtifactServerException(ExceptionUtils.getMessage(exception), USER);
        }
      }
    }
    return true;
  }

  private String getToken(DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, Headers headers,
      DockerRegistryRestClient registryRestClient) {
    encryptionService.decrypt(dockerConfig, encryptionDetails);
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
    String authHeaderValue = headers.get(AUTHENTICATE_HEADER);
    if (!cachedBearerTokens.containsKey(authHeaderValue)) {
      DockerRegistryToken dockerRegistryToken = fetchToken(registryRestClient, basicAuthHeader, authHeaderValue);
      if (dockerRegistryToken != null) {
        if (dockerRegistryToken.getExpires_in() != null) {
          cachedBearerTokens.put(authHeaderValue, dockerRegistryToken.getToken(), ExpirationPolicy.CREATED,
              dockerRegistryToken.getExpires_in(), TimeUnit.SECONDS);
        } else {
          return dockerRegistryToken.getToken();
        }
      }
    }
    return cachedBearerTokens.get(authHeaderValue);
  }

  private DockerRegistryToken fetchToken(
      DockerRegistryRestClient registryRestClient, String basicAuthHeader, String authHeaderValue) {
    try {
      Map<String, String> tokens = DockerRegistryUtils.extractAuthChallengeTokens(authHeaderValue);
      if (tokens != null) {
        DockerRegistryToken registryToken =
            registryRestClient
                .getToken(basicAuthHeader, tokens.get("realm"), tokens.get("service"), tokens.get("scope"))
                .execute()
                .body();
        if (registryToken != null) {
          tokens.putIfAbsent(authHeaderValue, registryToken.getToken());
          return registryToken;
        }
      }
    } catch (IOException e) {
      logger.warn("Exception occurred while fetching token", e);
    }
    return null;
  }

  public static boolean isSuccessful(Response<?> response) {
    if (response == null) {
      throw new InvalidArtifactServerException("Null response found", USER);
    }

    if (response.isSuccessful()) {
      return true;
    }

    logger.error("Request not successful. Reason: {}", response);
    int code = response.code();
    switch (code) {
      case 404:
      case 400:
        return false;
      case 401:
        throw new InvalidArtifactServerException("Invalid Docker Registry credentials", USER);
      default:
        throw new InvalidArtifactServerException(response.message(), USER);
    }
  }

  public static String parseLink(String headerLink) {
    /**
     * Traversing with the pagination e.g.
     * Link:
     * "</v2/myAccount/myfirstrepo/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol_oLvcYeti2w53XnoV3FYyFBkd-TQV3OBiWNJLqp2m8isy3SWusAqA4Y32dHJ7tGi0br18kXEt6nTW306QUFexaXrAGq8KeSc%3D&n=25>;
     * rel="next""
     */
    if (headerLink == null) {
      return null;
    }
    List<String> links = Arrays.stream(headerLink.split(";")).map(String::trim).collect(toList());

    // Replace space with empty string
    links.stream().map(s -> s.replace(" ", "")).collect(toList());
    if (!links.contains("rel=\"next\"")) {
      return null;
    }
    String path = null;
    for (String s : links) {
      if (s.charAt(0) == '<' && s.charAt(s.length() - 1) == '>') {
        path = s;
        break;
      }
    }
    if (path == null || path.length() <= 1) {
      return path;
    }

    String link = path.substring(1, path.length() - 1);

    try {
      URL url = new URL(link);
      link = url.getFile().substring(1);
    } catch (Exception e) {
      // In the case where the link isn't a valid URL, we were passed with the just relative path
    }
    return link.charAt(0) == '/' ? link.replaceFirst("/", "") : link;
  }

  public static String findNextLink(Headers headers) {
    if (headers == null || headers.size() == 0) {
      return null;
    }
    if (headers.get("link") == null) {
      return null;
    }

    return parseLink(headers.get("link"));
  }

  /**
   * The type Docker image tag response.
   */
  public static class DockerImageTagResponse {
    private String name;
    private List<String> tags;
    private String link;

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

    public String getLink() {
      return link;
    }

    public void setLink(String link) {
      this.link = link;
    }
  }

  /**
   * The type Docker registry token.
   */
  public static class DockerRegistryToken {
    private String token;
    private String access_token;
    private Integer expires_in;
    private String issued_at;

    /**
     * Gets token.
     *
     * @return the token
     */
    public String getToken() {
      return token;
    }

    /**
     * Sets token.
     *
     * @param token the token
     */
    public void setToken(String token) {
      this.token = token;
    }

    /**
     * Gets access token.
     *
     * @return the access token
     */
    public String getAccess_token() {
      return access_token;
    }

    /**
     * Sets access token.
     *
     * @param access_token the access token
     */
    public void setAccess_token(String access_token) {
      this.access_token = access_token;
    }

    /**
     * Gets expires in.
     *
     * @return the expires in
     */
    public Integer getExpires_in() {
      return expires_in;
    }

    /**
     * Sets expires in.
     *
     * @param expires_in the expires in
     */
    public void setExpires_in(Integer expires_in) {
      this.expires_in = expires_in;
    }

    /**
     * Gets issued at.
     *
     * @return the issued at
     */
    public String getIssued_at() {
      return issued_at;
    }

    /**
     * Sets issued at.
     *
     * @param issued_at the issued at
     */
    public void setIssued_at(String issued_at) {
      this.issued_at = issued_at;
    }
  }
}
