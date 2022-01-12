/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.constants;

public class AccountManagementConstants {
  public enum PermissionTypes {
    ACCOUNT_READONLY {
      public String toString() {
        return "ACCOUNT_READONLY";
      }
    },

    ACCOUNT_NOPERMISSION {
      public String toString() {
        return "ACCOUNT_NOPERMISSION";
      }
    },

    ACCOUNT_USERANDGROUPS {
      public String toString() {
        return "ACCOUNT_USERANDGROUPS";
      }
    },

    ACCOUNT_ADMIN {
      public String toString() {
        return "ACCOUNT_ADMIN";
      }
    },

    ACCOUNT_MANAGEMENT {
      public String toString() {
        return "ACCOUNT_MANAGEMENT";
      }
    },

    MANAGE_APPLICATIONS {
      public String toString() {
        return "MANAGE_APPLICATIONS";
      }
    }
  }
}
