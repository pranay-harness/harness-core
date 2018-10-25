package io.harness.limits.defaults.service;

import io.harness.limits.ActionType;
import io.harness.limits.lib.Limit;

public interface DefaultLimitsService { Limit get(ActionType actionType, String accountType); }
