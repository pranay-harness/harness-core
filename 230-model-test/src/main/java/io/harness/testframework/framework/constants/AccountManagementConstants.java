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
