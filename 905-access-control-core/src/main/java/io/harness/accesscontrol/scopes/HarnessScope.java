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
    public String getKey() {
      return "account";
    }

    @Override
    public String getIdentifierName() {
      return ACCOUNT_KEY;
    }

    @Override
    public String toString() {
      return getKey();
    }
  },

  ORGANIZATION {
    @Override
    public int getRank() {
      return 1;
    }

    @Override
    public String getKey() {
      return "org";
    }

    @Override
    public String getIdentifierName() {
      return ORG_KEY;
    }

    @Override
    public String toString() {
      return getKey();
    }
  },

  PROJECT {
    @Override
    public int getRank() {
      return 2;
    }

    @Override
    public String getKey() {
      return "project";
    }

    @Override
    public String getIdentifierName() {
      return PROJECT_KEY;
    }

    @Override
    public String toString() {
      return getKey();
    }
  }
}
