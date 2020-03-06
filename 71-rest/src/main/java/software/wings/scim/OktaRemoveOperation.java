package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class OktaRemoveOperation extends PatchOperation {
  @JsonProperty protected final JsonNode value;

  @JsonIgnore private ObjectMapper jsonObjectMapper = new ObjectMapper();

  public OktaRemoveOperation(
      @JsonProperty(value = "path") String path, @JsonProperty(value = "value") final JsonNode value) {
    super(path);
    this.value = value;
  }

  @Override
  public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException {
    ArrayList<T> removeObjects = new ArrayList<>(value.size());
    for (JsonNode node : value) {
      removeObjects.add(jsonObjectMapper.treeToValue(node, cls));
    }
    return removeObjects;
  }

  @Override
  public String getOpType() {
    return "remove";
  }
}
