package software.wings.search.framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

@Slf4j
@UtilityClass
public final class SearchEntityUtils {
  public static String mergeSettings(String baseSettingsString, String entitySettingsString) {
    JsonObject entitySettings = new JsonParser().parse(entitySettingsString).getAsJsonObject();
    JsonObject baseSettings = new JsonParser().parse(baseSettingsString).getAsJsonObject();
    JsonObject temp = entitySettings.get("mappings").getAsJsonObject().get("properties").getAsJsonObject();

    Set<Entry<String, JsonElement>> entrySet = temp.entrySet();

    for (Map.Entry<String, JsonElement> entry : entrySet) {
      baseSettings.get("mappings")
          .getAsJsonObject()
          .get("properties")
          .getAsJsonObject()
          .add(entry.getKey(), temp.get(entry.getKey()));
    }
    return baseSettings.toString();
  }

  public static Optional<String> convertToJson(Object object) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.setSerializationInclusion(Include.NON_EMPTY);
    try {
      String jsonString = mapper.writeValueAsString(object);
      return Optional.of(jsonString);
    } catch (JsonProcessingException e) {
      logger.error("Could not convert view to json", e);
      return Optional.empty();
    }
  }
}
