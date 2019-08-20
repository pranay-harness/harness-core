package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.unboundid.scim2.common.Path;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.PatchOpType;
import com.unboundid.scim2.common.utils.JsonUtils;
import software.wings.scim.PatchOperation.AddOperation;
import software.wings.scim.PatchOperation.RemoveOperation;
import software.wings.scim.PatchOperation.ReplaceOperation;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AddOperation.class, name = "Add")
  , @JsonSubTypes.Type(value = RemoveOperation.class, name = "Remove"),
      @JsonSubTypes.Type(value = ReplaceOperation.class, name = "Replace")
})
public abstract class PatchOperation {
  static final class AddOperation extends PatchOperation {
    @JsonProperty private final JsonNode value;

    @JsonCreator
    private AddOperation(@JsonProperty(value = "path") final Path path,
        @JsonProperty(value = "value", required = true) final JsonNode value) throws ScimException {
      super(path);
      this.value = value;
    }

    @Override
    public PatchOpType getOpType() {
      return PatchOpType.ADD;
    }

    @Override
    public <T> T getValue(final Class<T> cls) throws JsonProcessingException, ScimException, IllegalArgumentException {
      if (value.isArray()) {
        throw new IllegalArgumentException("Patch operation contains "
            + "multiple values");
      }
      return JsonUtils.getObjectReader().treeToValue(value, cls);
    }

    @Override
    public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException, ScimException {
      ArrayList<T> objects = new ArrayList<>(value.size());
      for (JsonNode node : value) {
        objects.add(JsonUtils.getObjectReader().treeToValue(node, cls));
      }
      return objects;
    }
  }

  static final class RemoveOperation extends PatchOperation {
    @JsonProperty private final JsonNode value;

    @JsonCreator
    private RemoveOperation(@JsonProperty(value = "path") final Path path,
        @JsonProperty(value = "value", required = true) final JsonNode value) throws ScimException {
      super(path);
      this.value = value;
    }

    @Override
    public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException {
      ArrayList<T> objects = new ArrayList<>(value.size());
      for (JsonNode node : value) {
        objects.add(JsonUtils.getObjectReader().treeToValue(node, cls));
      }
      return objects;
    }

    @Override
    public PatchOpType getOpType() {
      return PatchOpType.REMOVE;
    }
  }

  static final class ReplaceOperation extends PatchOperation {
    @JsonProperty private final JsonNode value;

    @JsonCreator
    private ReplaceOperation(@JsonProperty(value = "path") final Path path,
        @JsonProperty(value = "value", required = true) final JsonNode value) {
      super(path);
      this.value = value;
    }

    @Override
    public PatchOpType getOpType() {
      return PatchOpType.REPLACE;
    }

    @Override
    public <T> T getValue(final Class<T> cls) throws JsonProcessingException, IllegalArgumentException {
      if (value.isArray()) {
        throw new IllegalArgumentException("Patch operation contains "
            + "multiple values");
      }
      return JsonUtils.getObjectReader().treeToValue(value, cls);
    }

    @Override
    public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException {
      ArrayList<T> objects = new ArrayList<>(value.size());
      for (JsonNode node : value) {
        objects.add(JsonUtils.getObjectReader().treeToValue(node, cls));
      }
      return objects;
    }
  }

  private final Path path;

  PatchOperation(final Path path) {
    this.path = path;
  }

  @JsonIgnore public abstract PatchOpType getOpType();

  public Path getPath() {
    return path;
  }

  public <T> T getValue(final Class<T> cls) throws JsonProcessingException, ScimException, IllegalArgumentException {
    return null;
  }

  public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException, ScimException {
    return null;
  }

  public static PatchOperation add(final Path path, final JsonNode value) {
    try {
      return new AddOperation(path, value);
    } catch (ScimException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
