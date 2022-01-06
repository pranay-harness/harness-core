/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
