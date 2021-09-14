/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
