/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.customdeployment;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@Singleton
@UtilityClass
public class InstanceMapperUtils {
  public static final String hostname = "hostname";

  @NotNull
  public static <T> List<T> mapJsonToInstanceElements(Map<String, String> hostAttributes, String hostObjectArrayPath,
      String output, Function<HostProperties, T> jsonMapper) {
    final List<Map<String, Object>> instanceList = JsonUtils.jsonPath(output, hostObjectArrayPath);
    final String hostNameKey = hostAttributes.get(hostname);

    Function<Map<String, Object>, HostProperties> hostJsonPropertyMapper =
        getMapHostPropertiesFunction(hostAttributes, hostNameKey);

    if (isNotEmpty(instanceList)) {
      return instanceList.stream()
          .map(hostJsonPropertyMapper)
          .filter(hostProperties -> StringUtils.isNotBlank(hostProperties.getHostName()))
          .map(jsonMapper)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private Function<Map<String, Object>, HostProperties> getMapHostPropertiesFunction(
      Map<String, String> hostAttributes, String hostNameKey) {
    return instance -> {
      final String hostJson = JsonUtils.asJson(instance);
      Map<String, Object> otherHostProperties = hostAttributes.keySet().stream().collect(
          HashMap::new, (m, v) -> m.put(v, JsonUtils.jsonPath(hostJson, hostAttributes.get(v))), HashMap::putAll);

      return HostProperties.builder()
          .hostName(JsonUtils.jsonPath(JsonUtils.asJson(instance), hostNameKey))
          .otherPropeties(otherHostProperties)
          .build();
    };
  }

  public static String prettyJson(String json, String key) throws JsonProcessingException {
    return JsonUtils.asPrettyJson(JsonUtils.jsonPath(json, key));
  }

  public static String getHostnameFieldName(Map<String, String> attributes) {
    return attributes.get(hostname);
  }

  @Data
  @Builder
  public static final class HostProperties {
    private String hostName;
    private Map<String, Object> otherPropeties;
  }
}
