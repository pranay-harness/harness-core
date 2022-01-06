/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Created by rishi on 3/23/17.
 */
public class AccountRole {
  private String accountId;
  private String accountName;
  private boolean allApps;
  private ImmutableList<ApplicationRole> applicationRoles;
  private ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess;

  public String getAccountId() {
    return accountId;
  }

  void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAccountName() {
    return accountName;
  }

  void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public boolean isAllApps() {
    return allApps;
  }

  void setAllApps(boolean allApps) {
    this.allApps = allApps;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("accountName", accountName)
        .add("allApps", allApps)
        .add("applicationRoles", applicationRoles)
        .add("resourceAccess", resourceAccess)
        .toString();
  }

  public ImmutableList<ApplicationRole> getApplicationRoles() {
    return applicationRoles;
  }

  void setApplicationRoles(ImmutableList<ApplicationRole> applicationRoles) {
    this.applicationRoles = applicationRoles;
  }

  public ImmutableList<ImmutablePair<ResourceType, Action>> getResourceAccess() {
    return resourceAccess;
  }

  void setResourceAccess(ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess) {
    this.resourceAccess = resourceAccess;
  }

  public static final class AccountRoleBuilder {
    private String accountId;
    private String accountName;
    private boolean allApps;
    private ImmutableList<ApplicationRole> applicationRoles;
    private ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess;

    private AccountRoleBuilder() {}

    public static AccountRoleBuilder anAccountRole() {
      return new AccountRoleBuilder();
    }

    public AccountRoleBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public AccountRoleBuilder withAccountName(String accountName) {
      this.accountName = accountName;
      return this;
    }

    public AccountRoleBuilder withAllApps(boolean allApps) {
      this.allApps = allApps;
      return this;
    }

    public AccountRoleBuilder withApplicationRoles(ImmutableList<ApplicationRole> applicationRoles) {
      this.applicationRoles = applicationRoles;
      return this;
    }

    public AccountRoleBuilder withResourceAccess(ImmutableList<ImmutablePair<ResourceType, Action>> resourceAccess) {
      this.resourceAccess = resourceAccess;
      return this;
    }

    public AccountRole build() {
      AccountRole accountRole = new AccountRole();
      accountRole.setAccountId(accountId);
      accountRole.setAccountName(accountName);
      accountRole.setAllApps(allApps);
      accountRole.setApplicationRoles(applicationRoles);
      accountRole.setResourceAccess(resourceAccess);
      return accountRole;
    }
  }
}
