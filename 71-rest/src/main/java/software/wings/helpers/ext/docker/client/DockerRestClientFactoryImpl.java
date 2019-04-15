package software.wings.helpers.ext.docker.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.DockerRegistryRestClient;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl.DockerRegistryToken;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class DockerRestClientFactoryImpl implements DockerRestClientFactory {
  @Inject private EncryptionService encryptionService;
  private static ExpiringMap<String, String> cachedBearerTokens = ExpiringMap.builder().variableExpiration().build();

  @Override
  public DockerRegistryRestClient getDockerRegistryRestClient(
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

  public static String getToken(DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, Headers headers,
      DockerRegistryRestClient registryRestClient, EncryptionService encryptionService) {
    String basicAuthHeader = Credentials.basic(dockerConfig.getUsername(), new String(dockerConfig.getPassword()));
    String authHeaderValue = headers.get("Www-Authenticate");
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

  private static DockerRegistryToken fetchToken(
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
      logger.warn("Exception occurred while fetching token", e);
    }
    return null;
  }

  private static Map<String, String> extractAuthChallengeTokens(String authHeaderValue) {
    // Bearer realm="xxx",service="yyy",scope="zzz"

    if (authHeaderValue != null) {
      String[] headerParts = authHeaderValue.split(" ");

      if (headerParts.length == 2 && "Bearer".equals(headerParts[0])) {
        Map<String, String> tokens =
            Arrays.stream(headerParts[1].split(","))
                .map(token -> token.split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1].substring(1, s[1].length() - 1)));
        if (tokens.size() == 3 && tokens.get("realm") != null && tokens.get("service") != null
            && tokens.get("scope") != null) {
          return tokens;
        } else if (tokens.size() == 2 && tokens.get("realm") != null && tokens.get("service") != null) {
          return tokens;
        }
      }
    }
    return null;
  }
}