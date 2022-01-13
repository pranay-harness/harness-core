/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Created by rishi on 3/23/17.
 */
public class ApplicationRole {
  private String appId;
  private String appName;
  private boolean allEnvironments;
  private List<EnvironmentRole> environmentRoles;
  private ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess;

  public String getAppId() {
    return appId;
  }

  void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAppName() {
    return appName;
  }

  void setAppName(String appName) {
    this.appName = appName;
  }

  public boolean isAllEnvironments() {
    return allEnvironments;
  }

  void setAllEnvironments(boolean allEnvironments) {
    this.allEnvironments = allEnvironments;
  }

  public List<EnvironmentRole> getEnvironmentRoles() {
    return environmentRoles;
  }

  void setEnvironmentRoles(List<EnvironmentRole> environmentRoles) {
    this.environmentRoles = environmentRoles;
  }

  public ImmutableList<ImmutablePair<ResourceType, Action>> getResourceAccess() {
    return resourceAccess;
  }

  void setResourceAccess(ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess) {
    this.resourceAccess = resourceAccess;
  }

  public static final class ApplicationRoleBuilder {
    private String appId;
    private String appName;
    private boolean allEnvironments;
    private List<EnvironmentRole> environmentRoles;
    private ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess;

    private ApplicationRoleBuilder() {}

    public static ApplicationRoleBuilder anApplicationRole() {
      return new ApplicationRoleBuilder();
    }

    public ApplicationRoleBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ApplicationRoleBuilder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public ApplicationRoleBuilder withAllEnvironments(boolean allEnvironments) {
      this.allEnvironments = allEnvironments;
      return this;
    }

    public ApplicationRoleBuilder withEnvironmentRoles(List<EnvironmentRole> environmentRoles) {
      this.environmentRoles = environmentRoles;
      return this;
    }

    public ApplicationRoleBuilder withResourceAccess(
        ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess) {
      this.resourceAccess = resourceAccess;
      return this;
    }

    public ApplicationRole build() {
      ApplicationRole applicationRole = new ApplicationRole();
      applicationRole.setAppId(appId);
      applicationRole.setAppName(appName);
      applicationRole.setAllEnvironments(allEnvironments);
      applicationRole.setEnvironmentRoles(environmentRoles);
      applicationRole.setResourceAccess(resourceAccess);
      return applicationRole;
    }
  }
}
