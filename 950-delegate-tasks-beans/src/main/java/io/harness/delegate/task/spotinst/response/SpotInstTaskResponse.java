package io.harness.delegate.task.spotinst.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = SpotInstDeployTaskResponse.class, name = "spotInstDeployTaskResponse")
  , @JsonSubTypes.Type(value = SpotInstGetElastigroupJsonResponse.class, name = "spotInstGetElastigroupJsonResponse"),
      @JsonSubTypes.Type(
          value = SpotInstListElastigroupInstancesResponse.class, name = "sppotInstListElastigroupInstancesResponse"),
      @JsonSubTypes.Type(
          value = SpotInstListElastigroupNamesResponse.class, name = "spotInstListElastigroupNamesResponse"),
      @JsonSubTypes.Type(value = SpotInstSetupTaskResponse.class, name = "spotInstSetupTaskResponse"),
      @JsonSubTypes.Type(
          value = SpotinstTrafficShiftAlbDeployResponse.class, name = "spotinstTrafficShiftAlbDeployResponse"),
      @JsonSubTypes.Type(
          value = SpotinstTrafficShiftAlbSetupResponse.class, name = "spotinstTrafficShiftAlbSetupResponse"),
})
public interface SpotInstTaskResponse {}
