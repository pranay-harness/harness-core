package software.wings.sm.states.customdeployment;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.harness.serializer.JsonUtils;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
@UtilityClass
public class InstanceElementMapperUtils {
  public static final String hostname = "hostname";

  @VisibleForTesting
  @NotNull
  public static <T> List<T> mapJsonToInstanceElements(
      Map<String, String> hostAttributes, String hostObjectArrayPath, String output, Function<String, T> jsonMapper) {
    final List<Map<String, Object>> instanceList = JsonUtils.jsonPath(output, hostObjectArrayPath);
    final String hostNameKey = (String) hostAttributes.get(hostname);
    if (isNotEmpty(instanceList)) {
      return instanceList.stream()
          .map(JsonUtils::asJson)
          .map(v -> (String) JsonUtils.jsonPath(v, hostNameKey))
          .map(jsonMapper::apply)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public static String prettyJson(String json, String key) throws JsonProcessingException {
    return JsonUtils.asPrettyJson(JsonUtils.jsonPath(json, key));
  }

  public static String getHostnameFieldName(Map<String, String> attributes) {
    return attributes.get(hostname);
  }
}
