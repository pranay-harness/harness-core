package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by anubhaw on 5/16/16.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = HostAttributes.class, name = "HOST_ATTRIBUTES") })
public abstract class EnvironmentValue {
  public enum EnvironmentVariableTypes { HOST_ATTRIBUTES }

  private EnvironmentVariableTypes type;

  public EnvironmentValue(EnvironmentVariableTypes type) {
    this.type = type;
  }
}
