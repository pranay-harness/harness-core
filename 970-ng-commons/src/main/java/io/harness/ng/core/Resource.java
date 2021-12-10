package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@Builder
@JsonInclude(NON_NULL)
@FieldNameConstants(innerTypeName = "ResourceKeys")
public class Resource {
  @NotEmpty Type type;
  @NotEmpty String identifier;
  Map<String, String> labels;

  public enum Type {
    @JsonProperty("ORGANIZATION") ORGANIZATION,
    @JsonProperty("PROJECT") PROJECT,
    @JsonProperty("USER_GROUP") USER_GROUP,
    @JsonProperty("SECRET") SECRET,
    @JsonProperty("RESOURCE_GROUP") RESOURCE_GROUP,
    @JsonProperty("USER") USER,
    @JsonProperty("ROLE") ROLE,
    @JsonProperty("ROLE_ASSIGNMENT") ROLE_ASSIGNMENT,
    @JsonProperty("PIPELINE") PIPELINE,
    @JsonProperty("TRIGGER") TRIGGER,
    @JsonProperty("TEMPLATE") TEMPLATE,
    @JsonProperty("INPUT_SET") INPUT_SET,
    @JsonProperty("DELEGATE_CONFIGURATION") DELEGATE_CONFIGURATION,
    @JsonProperty("SERVICE") SERVICE,
    @JsonProperty("ENVIRONMENT") ENVIRONMENT,
    @JsonProperty("DELEGATE") DELEGATE,
    @JsonProperty("SERVICE_ACCOUNT") SERVICE_ACCOUNT,
    @JsonProperty("CONNECTOR") CONNECTOR,
    @JsonProperty("API_KEY") API_KEY,
    @JsonProperty("TOKEN") TOKEN,
    @JsonProperty("DELEGATE_TOKEN") DELEGATE_TOKEN;

    @JsonCreator
    public Type getFromString(String resourceType) {
      for (Type type : Type.values()) {
        if (type.name().equalsIgnoreCase(resourceType)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Invalid Resource.Type " + resourceType);
    }
  }
}
