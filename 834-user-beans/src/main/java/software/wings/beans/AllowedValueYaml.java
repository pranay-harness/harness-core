package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(PL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StringAllowedValue.class, name = "TEXT")
  , @JsonSubTypes.Type(value = NumberAllowedValue.class, name = "NUMBER"),
      @JsonSubTypes.Type(value = ArtifactStreamAllowedValueYaml.class, name = "ARTIFACT")
})
public interface AllowedValueYaml {}
