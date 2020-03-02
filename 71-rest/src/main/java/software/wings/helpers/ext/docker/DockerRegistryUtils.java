package software.wings.helpers.ext.docker;

import static io.harness.exception.WingsException.USER;
import static software.wings.helpers.ext.docker.DockerRegistryServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidCredentialsException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import org.apache.commons.lang3.tuple.ImmutablePair;
import retrofit2.Response;
import software.wings.exception.InvalidArtifactServerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class DockerRegistryUtils {
  @Inject private ExecutorService executorService;

  public List<Map<String, String>> getLabels(DockerRegistryRestClient registryRestClient,
      Function<Headers, String> getTokenFn, String authHeader, String imageName, List<String> tags) {
    Map<Integer, Map<String, String>> labelsMap = new ConcurrentHashMap<>();
    if (EmptyPredicate.isEmpty(tags)) {
      return Collections.emptyList();
    }

    final int size = tags.size();
    try {
      // Get labels for the first tag to get the latest auth header.
      String tag = tags.get(0);
      ImmutablePair<Map<String, String>, String> res =
          getSingleTagLabels(registryRestClient, getTokenFn, authHeader, imageName, tag);
      String finalAuthHeader = res.getRight();
      if (tags.size() <= 1) {
        return Collections.singletonList(res.getLeft());
      }
      labelsMap.put(0, res.getLeft());

      for (int i = 1; i < size; i++) {
        // Get labels for the next (at most) MAX_GET_LABELS_CONCURRENCY tags.
        String tagInternal = tags.get(i);
        try {
          Map<String, String> newLabels =
              getSingleTagLabels(registryRestClient, finalAuthHeader, imageName, tagInternal);
          labelsMap.put(i, newLabels);
        } catch (Exception e) {
          logger.error("Could not fetch docker labels for {}:{}", imageName, tagInternal, e);
        }
      }

    } catch (Exception e) {
      // Ignore error until we understand why fetching labels is failing sometimes.
      logger.error("Failed to fetch docker image labels", e);
      return Collections.emptyList();
    }

    int tagsProcessed = tags.size();
    List<Map<String, String>> labelsList = new ArrayList<>();
    for (int i = 0; i < tagsProcessed; i++) {
      Map<String, String> labels = labelsMap.getOrDefault(i, null);
      if (labels == null) {
        labels = new HashMap<>();
      }
      labelsList.add(labels);
    }
    return labelsList;
  }

  private static Map<String, String> getSingleTagLabels(
      DockerRegistryRestClient registryRestClient, String authHeader, String imageName, String tag) throws IOException {
    ImmutablePair<Map<String, String>, String> res =
        getSingleTagLabels(registryRestClient, null, authHeader, imageName, tag);
    return res.getLeft();
  }

  private static ImmutablePair<Map<String, String>, String> getSingleTagLabels(
      DockerRegistryRestClient registryRestClient, Function<Headers, String> getTokenFn, String authHeader,
      String imageName, String tag) throws IOException {
    Response<DockerImageManifestResponse> response =
        registryRestClient.getImageManifest(authHeader, imageName, tag).execute();
    if (response.code() == 401) { // unauthorized
      if (getTokenFn == null) {
        // We don't want to retry if getTokenFn is null.
        throw new InvalidCredentialsException("Invalid docker registry credentials", USER);
      }
      String token = getTokenFn.apply(response.headers());
      authHeader = "Bearer " + token;
      response = registryRestClient.getImageManifest(authHeader, imageName, tag).execute();
      if (response.code() == 401) {
        // Unauthorized even after retry.
        throw new InvalidCredentialsException("Invalid docker registry credentials", USER);
      }
    }

    if (!isSuccessful(response)) {
      throw new InvalidArtifactServerException(response.message(), USER);
    }

    checkValidImage(imageName, response);
    DockerImageManifestResponse dockerImageManifestResponse = response.body();
    if (dockerImageManifestResponse == null) {
      return ImmutablePair.of(new HashMap<>(), authHeader);
    }
    return ImmutablePair.of(dockerImageManifestResponse.fetchLabels(), authHeader);
  }

  static void checkValidImage(String imageName, Response response) {
    if (response.code() == 404) { // page not found
      throw new InvalidArgumentsException(
          ImmutablePair.of("code", "Image name [" + imageName + "] does not exist in Docker Registry."), null, USER);
    }
  }

  static Map<String, String> extractAuthChallengeTokens(String authHeaderValue) {
    // Bearer realm="xxx",service="yyy",scope="zzz"
    if (authHeaderValue != null) {
      String[] headerParts = authHeaderValue.split(" ");
      if (headerParts.length == 2 && "Bearer".equals(headerParts[0])) {
        Map<String, String> tokens =
            Arrays.stream(headerParts[1].split(","))
                .map(token -> token.split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1].substring(1, s[1].length() - 1)));
        if ((tokens.size() == 3 && tokens.get("realm") != null && tokens.get("service") != null
                && tokens.get("scope") != null)
            || (tokens.size() == 2 && tokens.get("realm") != null && tokens.get("service") != null)) {
          return tokens;
        }
      }
    }
    return null;
  }
}
