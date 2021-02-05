package io.harness.accesscontrol.scopes;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

public enum HarnessScope implements Scope {
  ACCOUNT {
    @Override
    public int getRank() {
      return 0;
    }

    @Override
    public String getPathKey() {
      return "accounts";
    }

    @Override
    public String getDBKey() {
      return ACCOUNT.name();
    }

    @Override
    public String getIdentifierKey() {
      return ACCOUNT_KEY;
    }
  },

  ORGANIZATION {
    @Override
    public int getRank() {
      return 1;
    }

    @Override
    public String getPathKey() {
      return "organizations";
    }

    @Override
    public String getDBKey() {
      return ORGANIZATION.name();
    }

    @Override
    public String getIdentifierKey() {
      return ORG_KEY;
    }
  },

  PROJECT {
    @Override
    public int getRank() {
      return 2;
    }

    @Override
    public String getPathKey() {
      return "projects";
    }

    @Override
    public String getDBKey() {
      return PROJECT.name();
    }

    @Override
    public String getIdentifierKey() {
      return PROJECT_KEY;
    }
  }
}
