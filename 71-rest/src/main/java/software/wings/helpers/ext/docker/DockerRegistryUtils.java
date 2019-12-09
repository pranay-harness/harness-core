package software.wings.helpers.ext.docker;

import static io.harness.exception.WingsException.USER;
import static software.wings.helpers.ext.docker.DockerRegistryServiceImpl.isSuccessful;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.exception.ArtifactServerException;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class DockerRegistryUtils {
  @Inject private ExecutorService executorService;

  private static final int MAX_GET_LABELS_CONCURRENCY = 20;

  public List<Map<String, String>> getLabels(DockerRegistryRestClient registryRestClient,
      Function<Headers, String> getTokenFn, String authHeader, String imageName, List<String> tags, long deadline) {
    Map<Integer, Map<String, String>> labelsMap = new ConcurrentHashMap<>();
    if (EmptyPredicate.isEmpty(tags)) {
      return Collections.emptyList();
    }

    final int size = tags.size();
    int start = 1;
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

      while (start < size) {
        // Get labels for the next (at most) MAX_GET_LABELS_CONCURRENCY tags.
        List<Callable<Boolean>> callables = new ArrayList<>();
        for (int i = start; i < size && i < start + MAX_GET_LABELS_CONCURRENCY; i++) {
          int index = i;
          callables.add(() -> {
            String tagInternal = tags.get(index);
            try {
              Map<String, String> newLabels =
                  getSingleTagLabels(registryRestClient, finalAuthHeader, imageName, tagInternal);
              labelsMap.put(index, newLabels);
              return true;
            } catch (Exception e) {
              logger.error("Could not fetch docker labels for {}:{}", imageName, tagInternal, e);
              return false;
            }
          });
        }

        // Wait for all the futures in this iteration with a deadline.
        long timeout = deadline - (new Date()).getTime();
        List<Future<Boolean>> futures = executorService.invokeAll(callables, timeout, TimeUnit.MILLISECONDS);
        boolean timeoutExceeded = false;
        boolean gotException = false;
        for (Future<Boolean> future : futures) {
          if (future.isCancelled() || !future.isDone()) {
            timeoutExceeded = true;
          } else if (!future.get()) {
            gotException = true;
          }
        }

        if (gotException) {
          throw new ArtifactServerException("Failed to fetch docker image labels");
        } else if (timeoutExceeded) {
          // Deadline exceeded, return with the labels for the processed tags.
          break;
        }
        start += MAX_GET_LABELS_CONCURRENCY;
      }
    } catch (Exception e) {
      // Ignore error until we understand why fetching labels is failing sometimes.
      logger.error("Failed to fetch docker image labels", e);
      return Collections.emptyList();
    }

    // The total number of tags that we have processed (collected labels for) might be less than tags.size() as we might
    // have hit the deadline. In that case we return a list of size tagsProcessed.
    int tagsProcessed = Math.min(tags.size(), start);
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
