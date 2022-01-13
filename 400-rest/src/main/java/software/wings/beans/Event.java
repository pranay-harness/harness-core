/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.common.base.MoreObjects;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
@TargetModule(HarnessModule._360_CG_MANAGER)
@OwnedBy(HarnessTeam.PL)
public class Event {
  private String orgId = "*";
  private String appId = "all";
  private String envId = "all";
  private String serviceId = "all";
  private Type type;
  private String uuid;

  /**
   * Getter for property 'orgId'.
   *
   * @return Value for property 'orgId'.
   */
  public String getOrgId() {
    return orgId;
  }

  /**
   * Setter for property 'orgId'.
   *
   * @param orgId Value to set for property 'orgId'.
   */
  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  /**
   * Getter for property 'appId'.
   *
   * @return Value for property 'appId'.
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Setter for property 'appId'.
   *
   * @param appId Value to set for property 'appId'.
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Getter for property 'envId'.
   *
   * @return Value for property 'envId'.
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Setter for property 'envId'.
   *
   * @param envId Value to set for property 'envId'.
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Getter for property 'serviceId'.
   *
   * @return Value for property 'serviceId'.
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Setter for property 'serviceId'.
   *
   * @param serviceId Value to set for property 'serviceId'.
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Getter for property 'type'.
   *
   * @return Value for property 'type'.
   */
  public Type getType() {
    return type;
  }

  /**
   * Setter for property 'type'.
   *
   * @param type Value to set for property 'type'.
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Getter for property 'uuid'.
   *
   * @return Value for property 'uuid'.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Setter for property 'uuid'.
   *
   * @param uuid Value to set for property 'uuid'.
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(orgId, appId, envId, serviceId, type, uuid);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Event other = (Event) obj;
    return Objects.equals(this.orgId, other.orgId) && Objects.equals(this.appId, other.appId)
        && Objects.equals(this.envId, other.envId) && Objects.equals(this.serviceId, other.serviceId)
        && this.type == other.type && Objects.equals(this.uuid, other.uuid);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("orgId", orgId)
        .add("appId", appId)
        .add("envId", envId)
        .add("serviceId", serviceId)
        .add("type", type)
        .add("uuid", uuid)
        .toString();
  }

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Create type.
     */
    CREATE,
    /**
     * Update type.
     */
    UPDATE,
    /**
     * Delete type.
     */
    DELETE,
    /**
     * Enable type.
     */
    ENABLE,
    /**
     * Disable type.
     */
    DISABLE,
    /**
     * Lock type.
     */
    LOCK,
    /**
     * Unlock type.
     */
    UNLOCK,
    /**
     * Reset Password type.
     */
    RESET_PASSWORD,
    /**
     * Accepted Invite type.
     */
    ACCEPTED_INVITE,
    /**
     * Enable 2FA type.
     */
    ENABLE_2FA,
    /**
     * Disable 2FA type.
     */
    DISABLE_2FA,
    /**
     * 2FA Login
     */
    LOGIN_2FA,
    /**
     * Login type.
     */
    LOGIN,
    /**
     * Unsuccessful login type.
     */
    UNSUCCESSFUL_LOGIN,
    /**
     * Link SSO type.
     */
    LINK_SSO,
    /**
     * Unlink SSO type.
     */
    UNLINK_SSO,
    /**
     * Update Notification Setting type.
     */
    UPDATE_NOTIFICATION_SETTING,
    /**
     * Modify Permission type.
     */
    MODIFY_PERMISSIONS,
    /**
     * Test Connection type.
     */
    TEST,
    /**
     * Update type.
     */
    UPDATE_TAG,
    /**
     * Update Delegate Scope type.
     */
    UPDATE_SCOPE,

    /**
     * for addition of users in user groups and assigning user groups to users
     */
    ADD,

    /**
     * for removal of users from user groups and un assigning user groups to users
     */
    REMOVE,
    /**
     * Delegate Approval type.
     */
    DELEGATE_APPROVAL,
    /**
     * Delegate Rejection type.
     */
    DELEGATE_REJECTION,
    /**
     * Delegate Registration type.
     */
    DELEGATE_REGISTRATION,
    /**
     * for non-whitelisted users login attempt
     */
    NON_WHITELISTED,
    /**
     * for API Invocation using API_Key
     */
    INVOKED,
    /**
     * Apply Delegate Profile
     */
    APPLY
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String orgId = "*";
    private String appId = "all";
    private String envId = "all";
    private String serviceId = "all";
    private Type type;
    private String uuid;

    private Builder() {}

    /**
     * An event builder.
     *
     * @return the builder
     */
    public static Builder anEvent() {
      return new Builder();
    }

    /**
     * With org id builder.
     *
     * @param orgId the org id
     * @return the builder
     */
    public Builder withOrgId(String orgId) {
      this.orgId = orgId;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(Type type) {
      this.type = type;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anEvent()
          .withOrgId(orgId)
          .withAppId(appId)
          .withEnvId(envId)
          .withServiceId(serviceId)
          .withType(type)
          .withUuid(uuid);
    }

    /**
     * Build event.
     *
     * @return the event
     */
    public Event build() {
      Event event = new Event();
      event.setOrgId(orgId);
      event.setAppId(appId);
      event.setEnvId(envId);
      event.setServiceId(serviceId);
      event.setType(type);
      event.setUuid(uuid);
      return event;
    }
  }
}
