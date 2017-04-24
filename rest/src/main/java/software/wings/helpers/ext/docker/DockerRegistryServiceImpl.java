package software.wings.helpers.ext.docker;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Singleton;

import java.util.HashMap;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import okhttp3.Credentials;
import okhttp3.Headers;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DockerConfig;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 1/6/17.
 */
@Singleton
public class DockerRegistryServiceImpl implements DockerRegistryService {
  private ExpiringMap<String, String> cachedBearerTokens = ExpiringMap.builder().variableExpiration().build();

  private DockerRegistryRestClient getDockerRegistryRestClient(DockerConfig dockerConfig) {
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(dockerConfig.getDockerRegistryUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    DockerRegistryRestClient dockerRegistryRestClient = retrofit.create(DockerRegistryRestClient.class);
    return dockerRegistryRestClient;
  }

  @Override
  public List<BuildDetails> getBuilds(DockerConfig dockerConfig, String imageName, int maxNumberOfBuilds) {
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
    try {
      DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(dockerConfig);

      Response<DockerImageTagResponse> response =
          registryRestClient.listImageTags(basicAuthHeader, imageName).execute();

      if (response.code() == 401) { // unauthorized
        String token = getToken(dockerConfig, response.headers(), registryRestClient);
        response = registryRestClient.listImageTags("Bearer " + token, imageName).execute();
      }
      checkValidImage(imageName, response);
      return processBuildResponse(response.body());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void checkValidImage(String imageName, Response<DockerImageTagResponse> response) {
    if (response.code() == 404) { // Page not found
      Map<String, Object> params = new HashMap<>();
      params.put("name", imageName);
      params.put("reason", " Reason: Image name does not exist.");
      throw new WingsException(params, ErrorCode.INVALID_ARTIFACT_SOURCE);
    }
  }

  private List<BuildDetails> processBuildResponse(DockerImageTagResponse dockerImageTagResponse) {
    return dockerImageTagResponse.getTags()
        .stream()
        .map(s -> aBuildDetails().withNumber(s).build())
        .collect(Collectors.toList());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(DockerConfig dockerConfig, String imageName) {
    return null;
  }

  @Override
  public void verifyImageName(DockerConfig dockerConfig, String imageName) {
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
    try {
      DockerRegistryRestClient registryRestClient = getDockerRegistryRestClient(dockerConfig);
      Response<DockerImageTagResponse> response =
          registryRestClient.listImageTags(basicAuthHeader, imageName).execute();
      if (response.code() == 401) { // unauthorized
        String token = getToken(dockerConfig, response.headers(), registryRestClient);
        response = registryRestClient.listImageTags("Bearer " + token, imageName).execute();
      }
      if (response.code() == 404) { // Page not found
        throw new WingsException(
            ErrorCode.INVALID_ARGUMENT, "args", "Image name [" + imageName + "] does not exist in Docker registry.");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getToken(DockerConfig dockerConfig, Headers headers, DockerRegistryRestClient registryRestClient) {
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
    String authHeaderValue = headers.get("Www-Authenticate");
    if (!cachedBearerTokens.containsKey(authHeaderValue)) {
      DockerRegistryToken dockerRegistryToken = fetchToken(registryRestClient, basicAuthHeader, authHeaderValue);
      if (dockerRegistryToken != null) {
        cachedBearerTokens.put(authHeaderValue, dockerRegistryToken.getToken(), ExpirationPolicy.CREATED,
            dockerRegistryToken.getExpires_in(), TimeUnit.SECONDS);
      }
    }
    return cachedBearerTokens.get(authHeaderValue);
  }

  private DockerRegistryToken fetchToken(
      DockerRegistryRestClient registryRestClient, String basicAuthHeader, String authHeaderValue) {
    try {
      Map<String, String> tokens = extractAuthChallengeTokens(authHeaderValue);
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
      e.printStackTrace();
    }
    return null;
  }

  private Map<String, String> extractAuthChallengeTokens(String authHeaderValue) {
    // Bearer realm="xxx",service="yyy",scope="zzz"

    if (authHeaderValue != null) {
      String[] headerParts = authHeaderValue.split(" ");

      if (headerParts.length == 2 && "Bearer".equals(headerParts[0])) {
        Map<String, String> tokens =
            Arrays.stream(headerParts[1].split(","))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1].substring(1, s[1].length() - 1)));

        if (tokens.size() == 3 && tokens.get("realm") != null && tokens.get("service") != null
            && tokens.get("scope") != null) {
          return tokens;
        }
      }
    }
    return null;
  }

  /**
   * The type Docker image tag response.
   */
  public static class DockerImageTagResponse {
    private String name;
    private List<String> tags;

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
