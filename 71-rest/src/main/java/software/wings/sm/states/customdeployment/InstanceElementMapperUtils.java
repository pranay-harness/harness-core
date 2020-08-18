package software.wings.sm.states.customdeployment;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.serializer.JsonUtils;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@UtilityClass
public class InstanceElementMapperUtils {
  public static final String hostname = "hostname";

  @VisibleForTesting
  public static <T> List<T> mapJsonToInstanceElements(
      Map<String, String> hostAttributes, String hostObjectArrayPath, String output, Function<String, T> jsonMapper) {
    final List<Map<String, Object>> instanceList = JsonUtils.jsonPath(output, hostObjectArrayPath);
    final String hostNameKey = (String) hostAttributes.get(hostname);
    if (EmptyPredicate.isNotEmpty(instanceList)) {
      return instanceList.stream()
          .map(element -> (String) element.get(hostNameKey))
          .map(jsonMapper::apply)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
