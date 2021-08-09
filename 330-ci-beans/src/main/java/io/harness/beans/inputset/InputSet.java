package io.harness.beans.inputset;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public interface InputSet {
  enum Type { Manual, Webhook }

  InputSet.Type getType();
}
