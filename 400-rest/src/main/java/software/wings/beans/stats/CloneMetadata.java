/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.stats;

import software.wings.beans.Environment;
import software.wings.beans.Workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 9/8/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloneMetadata {
  String targetAppId;
  Map<String, String> serviceMapping;
  Workflow workflow;
  Environment environment;
}
