package io.harness.framework.email;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "error_codes"})
public class Auth {
  @JsonProperty("success") private boolean success;
  @JsonProperty("error_codes") private List<Object> errorCodes = null;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("success")
  public boolean isSuccess() {
    return success;
  }

  @JsonProperty("success")
  public void setSuccess(boolean success) {
    this.success = success;
  }

  @JsonProperty("error_codes")
  public List<Object> getErrorCodes() {
    return errorCodes;
  }

  @JsonProperty("error_codes")
  public void setErrorCodes(List<Object> errorCodes) {
    this.errorCodes = errorCodes;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
