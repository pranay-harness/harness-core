package software.wings.service.intfc.deployment;

import com.sun.istack.Nullable;

public interface PreDeploymentChecker { void check(String accountId, @Nullable String appId); }
