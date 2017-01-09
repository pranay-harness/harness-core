package software.wings.helpers.ext.docker;

import com.google.inject.Singleton;

import okhttp3.Credentials;
import okhttp3.Headers;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 1/6/17.
 */
@Singleton
public class DockerRegistryServiceImpl implements DockerRegistryService {
  private ConcurrentMap<String, String> cachedBearerTokens = new ConcurrentHashMap<>();

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
      List<BuildDetails> buildDetailss = processBuildResponse(response.body());
      return buildDetailss;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private List<BuildDetails> processBuildResponse(DockerImageTagResponse dockerImageTagResponse) {
    return dockerImageTagResponse.getTags().stream().map(s -> new DockerBuildDetails(s)).collect(Collectors.toList());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(DockerConfig dockerConfig, String imageName) {
    return null;
  }

  private String getToken(DockerConfig dockerConfig, Headers headers, DockerRegistryRestClient registryRestClient) {
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), dockerConfig.getPassword());
    String authHeaderValue = headers.get("Www-Authenticate");
    return cachedBearerTokens.computeIfAbsent(
        authHeaderValue, s -> fetchToken(registryRestClient, basicAuthHeader, authHeaderValue));
  }

  private String fetchToken(
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
          return registryToken.getToken();
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

  public static class DockerImageTagResponse {
    private String name;
    private List<String> tags;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getTags() {
      return tags;
    }

    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }

  public static class DockerRegistryToken {
    private String token;
    private String access_token;
    private String expires_in;
    private String issued_at;

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getAccess_token() {
      return access_token;
    }

    public void setAccess_token(String access_token) {
      this.access_token = access_token;
    }

    public String getExpires_in() {
      return expires_in;
    }

    public void setExpires_in(String expires_in) {
      this.expires_in = expires_in;
    }

    public String getIssued_at() {
      return issued_at;
    }

    public void setIssued_at(String issued_at) {
      this.issued_at = issued_at;
    }
  }
}
