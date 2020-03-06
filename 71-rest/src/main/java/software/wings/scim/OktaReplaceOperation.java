package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class OktaReplaceOperation extends PatchOperation {
  @JsonProperty private final JsonNode value;

  @JsonIgnore private ObjectMapper jsonObjectMapper = new ObjectMapper();

  @JsonCreator
  public OktaReplaceOperation(
      @JsonProperty(value = "path") String path, @JsonProperty(value = "value") final JsonNode value) {
    super(path);
    this.value = value;
  }

  @Override
  public String getOpType() {
    return "replace";
  }

  @Override
  public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException {
    ArrayList<T> replaceObjects = new ArrayList<>(value.size());
    for (JsonNode node : value) {
      replaceObjects.add(jsonObjectMapper.treeToValue(node, cls));
    }
    return replaceObjects;
  }

  @Override
  public <T> T getValue(final Class<T> cls) throws JsonProcessingException {
    if (value.isArray()) {
      throw new IllegalArgumentException("Replace Patch operation contains "
          + "multiple values");
    }
    return jsonObjectMapper.treeToValue(value, cls);
  }
}