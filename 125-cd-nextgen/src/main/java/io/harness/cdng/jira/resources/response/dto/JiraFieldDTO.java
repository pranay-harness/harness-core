package io.harness.cdng.jira.resources.response.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "JiraField")
public class JiraFieldDTO {
  String key;
  String name;
  boolean required;
  boolean isCustom;
  JSONObject schema;
  JSONArray allowedValues;
}
