package io.harness.accesscontrol.scopes.harness;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;

import io.harness.accesscontrol.scopes.core.ScopeLevel;

public enum HarnessScopeLevel implements ScopeLevel {
  ACCOUNT {
    @Override
    public String toString() {
      return "account";
    }

    @Override
    public int getRank() {
      return 0;
    }

    @Override
    public String getParamName() {
      return ACCOUNT_LEVEL_PARAM_NAME;
    }
  },

  ORGANIZATION {
    @Override
    public String toString() {
      return "organization";
    }

    @Override
    public int getRank() {
      return 1;
    }

    @Override
    public String getParamName() {
      return ORG_LEVEL_PARAM_NAME;
    }
  },

  PROJECT {
    @Override
    public String toString() {
      return "project";
    }

    @Override
    public int getRank() {
      return 2;
    }

    @Override
    public String getParamName() {
      return PROJECT_LEVEL_PARAM_NAME;
    }
  }
}
