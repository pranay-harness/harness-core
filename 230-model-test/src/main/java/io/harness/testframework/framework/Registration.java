/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Registration {
  @SerializedName("accountId") @Expose private String accountId;
  @SerializedName("agreement") @Expose private boolean agreement;
  @SerializedName("email") @Expose private String email;
  @SerializedName("name") @Expose private String name;
  @SerializedName("password") @Expose private String password;
  @SerializedName("uuid") @Expose private String uuid;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public boolean isAgreement() {
    return agreement;
  }

  public void setAgreement(boolean agreement) {
    this.agreement = agreement;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
}
