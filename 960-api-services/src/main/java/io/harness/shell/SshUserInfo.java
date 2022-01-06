/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Created by anubhaw on 6/21/16.
 */
public class SshUserInfo implements UserInfo, UIKeyboardInteractive {
  private String password;
  private String passphrase;

  /**
   * Instantiates a new Ssh user info.
   *
   * @param password the password
   */
  public SshUserInfo(String password) {
    this.password = password;
  }

  @Override
  public String getPassphrase() {
    return passphrase;
  }

  /**
   * Sets passphrase.
   *
   * @param passphrase the passphrase
   */
  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  @Override
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean promptPassword(String message) {
    return true;
  }

  @Override
  public boolean promptPassphrase(String message) {
    return true;
  }

  @Override
  public boolean promptYesNo(String message) {
    return true;
  }

  @Override
  public void showMessage(String message) {
    // do nothing
  }

  @Override
  public String[] promptKeyboardInteractive(
      String destination, String name, String instruction, String[] prompt, boolean[] echo) {
    if (prompt.length != 1 || echo[0] || this.password == null) {
      return null;
    }
    String[] response = new String[1];
    response[0] = this.password;
    return response;
  }
}
