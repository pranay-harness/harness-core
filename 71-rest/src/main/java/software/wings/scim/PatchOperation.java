package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "op")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AddOperation.class, name = "Add")
  , @JsonSubTypes.Type(value = RemoveOperation.class, name = "Remove"),
      @JsonSubTypes.Type(value = ReplaceOperation.class, name = "Replace"),
      @JsonSubTypes.Type(value = OktaAddOperation.class, name = "add"),
      @JsonSubTypes.Type(value = OktaRemoveOperation.class, name = "remove"),
      @JsonSubTypes.Type(value = OktaReplaceOperation.class, name = "replace")
})
public abstract class PatchOperation {
  @JsonProperty private String path;

  PatchOperation(final String path) {
    this.path = path;
  }

  @JsonIgnore public abstract String getOpType();

  public String getPath() {
    return path;
  }

  public <T> T getValue(final Class<T> cls) throws JsonProcessingException, IllegalArgumentException {
    return null;
  }

  public <T> List<T> getValues(final Class<T> cls) throws JsonProcessingException {
    return null;
  }
}
