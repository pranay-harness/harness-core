package io.harness.scim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class ReplaceOperation extends PatchOperation {
  @JsonProperty private final JsonNode value;

  @JsonIgnore private ObjectMapper jsonObjectMapper = new ObjectMapper();

  @JsonCreator
  public ReplaceOperation(@JsonProperty(value = "path", required = true) String path,
      @JsonProperty(value = "value", required = true) final JsonNode value) {
    super(path);
    this.value = value;
  }

  @Override
  public String getOpType() {
    return "Replace";
  }

  @Override
  public <T> T getValue(final Class<T> cls) throws JsonProcessingException, IllegalArgumentException {
    if (value.isArray()) {
      throw new IllegalArgumentException("Patch operation contains "
          + "multiple values");
    }
    return jsonObjectMapper.treeToValue(value, cls);
  }

  @Override
  public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException {
    ArrayList<T> objects = new ArrayList<>(value.size());
    for (JsonNode node : value) {
      objects.add(jsonObjectMapper.treeToValue(node, cls));
    }
    return objects;
  }
}
