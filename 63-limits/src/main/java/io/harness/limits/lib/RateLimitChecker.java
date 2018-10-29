package io.harness.limits.lib;

import io.harness.limits.impl.model.RateLimit;

public interface RateLimitChecker extends LimitChecker { RateLimit getLimit(); }
