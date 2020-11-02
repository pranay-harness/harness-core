package io.harness.artifacts.docker.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorAscending;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.artifacts.docker.DockerRegistryRestClient;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.artifacts.docker.client.DockerRestClientFactory;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.WingsException;
import io.harness.expression.RegexFunctor;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import okhttp3.Credentials;
import okhttp3.Headers;
import org.apache.commons.lang3.tuple.ImmutablePair;
import retrofit2.Response;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 1/6/17.
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
public class DockerRegistryServiceImpl implements DockerRegistryService {
  public static final String BEARER = "Bearer ";
  @Inject private DockerPublicRegistryProcessor dockerPublicRegistryProcessor;
  @Inject private DockerRegistryUtils dockerRegistryUtils;
  @Inject private DockerRestClientFactory dockerRestClientFactory;
  private static final String AUTHENTICATE_HEADER = "Www-Authenticate";
  private static final int MAX_NUMBER_OF_BUILDS = 250;

  private ExpiringMap<String, String> cachedBearerTokens = ExpiringMap.builder().variableExpiration().build();

  @Override
  public List<BuildDetailsInternal> getBuilds(
      DockerInternalConfig dockerConfig, String imageName, int maxNumberOfBuilds) {
    List<BuildDetailsInternal> buildDetails;
    try {
      if (dockerConfig.hasCredentials()) {
        buildDetails = getBuildDetails(dockerConfig, imageName);
      } else {
        buildDetails = dockerPublicRegistryProcessor.getBuilds(dockerConfig, imageName, maxNumberOfBuilds);
      }
    } catch (Exception e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, WingsException.USER);
    }
    // Sorting at build tag for docker artifacts.
    // Don't change this order.
    return buildDetails.stream().sorted(new BuildDetailsInternalComparatorAscending()).collect(toList());
  }

  private List<BuildDetailsInternal> getBuildDetails(DockerInternalConfig dockerConfig, String imageName)
      throws IOException {
    DockerRegistryRestClient registryRestClient = dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig);
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
    List<BuildDetailsInternal> buildDetails = new ArrayList<>();
    String token = null;
    Response<DockerImageTagResponse> response = registryRestClient.listImageTags(basicAuthHeader, imageName).execute();
    if (response.code() == 401) { // unauthorized
      token = getToken(dockerConfig, response.headers(), registryRestClient);
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
      log.warn("There are no tags available for the imageName {}", imageName);
      return buildDetails;
    }
    buildDetails.addAll(processBuildResponse(dockerImageTagResponse, dockerConfig, imageName));
    // TODO: Limit the no of tags
    String baseUrl = response.raw().request().url().toString();
    while (true) {
      String nextLink = findNextLink(response.headers());
      if (nextLink == null) {
        if (buildDetails.size() > MAX_NO_OF_TAGS_PER_IMAGE) {
          buildDetails.subList(0, buildDetails.size() - MAX_NO_OF_TAGS_PER_IMAGE).clear();
        }
        return buildDetails;
      } else {
        log.info("Using pagination to fetch all the builds. The no of builds fetched so far {}", buildDetails.size());
      }
      int queryParamIndex = nextLink.indexOf('?');
      String nextPageUrl =
          queryParamIndex == -1 ? baseUrl.concat(nextLink) : baseUrl.concat(nextLink.substring(queryParamIndex));
      response = registryRestClient.listImageTagsByUrl(BEARER + token, nextPageUrl).execute();
      if (response.code() == 401) { // unauthorized
        token = getToken(dockerConfig, response.headers(), registryRestClient);
        response = registryRestClient.listImageTagsByUrl(BEARER + token, nextPageUrl).execute();
      }
      dockerImageTagResponse = response.body();
      if (dockerImageTagResponse == null || isEmpty(dockerImageTagResponse.getTags())) {
        log.info("There are no more tags available for the imageName {}. Returning tags", imageName);
        return buildDetails;
      }
      buildDetails.addAll(processBuildResponse(dockerImageTagResponse, dockerConfig, imageName));
      if (buildDetails.size() > MAX_NO_OF_TAGS_PER_IMAGE) {
        log.warn(
            "Image name {} has more than {} tags. We might miss some new tags", imageName, MAX_NO_OF_TAGS_PER_IMAGE);
        buildDetails.subList(0, buildDetails.size() - MAX_NO_OF_TAGS_PER_IMAGE).clear();
        break;
      }
    }
    return buildDetails;
  }

  private List<BuildDetailsInternal> processBuildResponse(
      DockerImageTagResponse dockerImageTagResponse, DockerInternalConfig dockerConfig, String imageName) {
    String tagUrl = dockerConfig.getDockerRegistryUrl().endsWith("/")
        ? dockerConfig.getDockerRegistryUrl() + imageName + "/tags/"
        : dockerConfig.getDockerRegistryUrl() + "/" + imageName + "/tags/";

    String domainName = Http.getDomainWithPort(dockerConfig.getDockerRegistryUrl());

    return dockerImageTagResponse.getTags()
        .stream()
        .map(tag -> {
          Map<String, String> metadata = new HashMap<>();
          metadata.put(ArtifactMetadataKeys.IMAGE, domainName + "/" + imageName + ":" + tag);
          metadata.put(ArtifactMetadataKeys.TAG, tag);
          return BuildDetailsInternal.builder()
              .number(tag)
              .buildUrl(tagUrl + tag)
              .uiDisplayName("Tag# " + tag)
              .metadata(metadata)
              .build();
        })
        .collect(toList());
  }

  @Override
  public List<Map<String, String>> getLabels(
      DockerInternalConfig dockerConfig, String imageName, List<String> buildNos) {
    if (!dockerConfig.hasCredentials()) {
      return dockerPublicRegistryProcessor.getLabels(dockerConfig, imageName, buildNos);
    }

    DockerRegistryRestClient registryRestClient = dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig);
    String authHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
    Function<Headers, String> getToken = headers -> getToken(dockerConfig, headers, registryRestClient);
    return dockerRegistryUtils.getLabels(registryRestClient, getToken, authHeader, imageName, buildNos);
  }

  @Override
  public BuildDetailsInternal getLastSuccessfulBuild(DockerInternalConfig dockerConfig, String imageName) {
    return null;
  }

  @Override
  public BuildDetailsInternal getLastSuccessfulBuildFromRegex(
      DockerInternalConfig dockerConfig, String imageName, String tagRegex) {
    List<BuildDetailsInternal> builds = getBuilds(dockerConfig, imageName, MAX_NUMBER_OF_BUILDS);
    builds = builds.stream()
                 .filter(build -> new RegexFunctor().match(tagRegex, build.getNumber()))
                 .sorted(new BuildDetailsInternalComparatorDescending())
                 .collect(Collectors.toList());

    if (builds.isEmpty()) {
      throw new InvalidArtifactServerException(
          "There are no builds for this image: " + imageName + " and tagRegex: " + tagRegex, USER);
    }
    // return the last build as builds list is sorted by ascending order
    return builds.get(0);
  }

  @Override
  public boolean verifyImageName(DockerInternalConfig dockerConfig, String imageName) {
    if (dockerConfig.hasCredentials()) {
      return checkImageName(dockerConfig, imageName);
    }
    return dockerPublicRegistryProcessor.verifyImageName(dockerConfig, imageName);
  }

  @Override
  public BuildDetailsInternal verifyBuildNumber(DockerInternalConfig dockerConfig, String imageName, String tag) {
    try {
      if (!dockerConfig.hasCredentials()) {
        return dockerPublicRegistryProcessor.verifyBuildNumber(dockerConfig, imageName, tag);
      }
      return getBuildNumber(dockerConfig, imageName, tag);
    } catch (IOException e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, USER);
    }
  }

  private boolean checkImageName(DockerInternalConfig dockerConfig, String imageName) {
    try {
      DockerRegistryRestClient registryRestClient = dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig);
      String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
      Response<DockerImageTagResponse> response =
          registryRestClient.listImageTags(basicAuthHeader, imageName).execute();
      if (response.code() == 401) { // unauthorized
        String token = getToken(dockerConfig, response.headers(), registryRestClient);
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

  private BuildDetailsInternal getBuildNumber(DockerInternalConfig dockerConfig, String imageName, String tag) {
    try {
      List<BuildDetailsInternal> builds = getBuildDetails(dockerConfig, imageName);
      builds = builds.stream().filter(build -> build.getNumber().equals(tag)).collect(Collectors.toList());

      if (builds.size() != 1) {
        throw new InvalidArtifactServerException("Didn't get build number", USER);
      }
      return builds.get(0);
    } catch (IOException e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, USER);
    }
  }

  @Override
  public boolean validateCredentials(DockerInternalConfig dockerConfig) {
    if (!connectableHttpUrl(dockerConfig.getDockerRegistryUrl())) {
      throw new InvalidArtifactServerException(
          "Could not reach Docker Registry at : " + dockerConfig.getDockerRegistryUrl(), USER);
    }
    if (dockerConfig.hasCredentials()) {
      if (isEmpty(dockerConfig.getPassword())) {
        throw new InvalidArtifactServerException("Password is a required field along with Username", USER);
      }
      DockerRegistryRestClient registryRestClient = null;
      String basicAuthHeader;
      String authHeaderValue;
      Response response;
      DockerRegistryToken dockerRegistryToken;
      try {
        registryRestClient = dockerRestClientFactory.getDockerRegistryRestClient(dockerConfig);
        basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
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
        log.warn("Failed to fetch apiversion with credentials" + e);
        try {
          // This is special case for repositories that require "/v2/" path for getting API version . Eg. Harbor docker
          // registry We get an IO exception with '/v2' path so we are retrying with forward slash API
          basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
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

  private String getToken(
      DockerInternalConfig dockerConfig, Headers headers, DockerRegistryRestClient registryRestClient) {
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
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
      log.warn("Exception occurred while fetching token", e);
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

    log.error("Request not successful. Reason: {}", response);
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
