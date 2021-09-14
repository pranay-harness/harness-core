/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.verification.log;

import software.wings.verification.CVConfiguration;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by Pranjal on 03/29/2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BugsnagCVConfiguration extends LogsCVConfiguration {
  @Attributes(required = true, title = "Organization") private String orgId;

  @Attributes(required = true, title = "Project") protected String projectId;

  @Attributes(title = "Release Stage") protected String releaseStage;

  @Attributes(required = true, title = "Browser Application") protected boolean browserApplication;

  @Override
  public CVConfiguration deepCopy() {
    BugsnagCVConfiguration clonedConfig = new BugsnagCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setOrgId(this.getOrgId());
    clonedConfig.setProjectId(this.getProjectId());
    clonedConfig.setReleaseStage(this.getReleaseStage());
    clonedConfig.setBrowserApplication(this.browserApplication);
    clonedConfig.setQuery(this.getQuery());
    return clonedConfig;
  }
}
