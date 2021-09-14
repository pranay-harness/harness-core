/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static software.wings.beans.PreferenceType.DEPLOYMENT_PREFERENCE;

import io.harness.mongo.index.FdIndex;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User Preference model
 * @author rathna on 04/25/2018
 */

@Data
@JsonTypeName("DEPLOYMENT_PREFERENCE")
@EqualsAndHashCode(callSuper = false)
public class DeploymentPreference extends Preference {
  List<String> appIds;
  List<String> pipelineIds;
  List<String> workflowIds;
  List<String> serviceIds;
  List<String> envIds;
  List<String> status;
  String startTime;
  String endTime;
  boolean includeIndirectExecutions;
  HarnessTagFilter harnessTagFilter;
  private transient String uiDisplayTagString;

  @SchemaIgnore @FdIndex private List<String> keywords;

  public DeploymentPreference() {
    super(DEPLOYMENT_PREFERENCE.name());
  }
}
