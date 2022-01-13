/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
