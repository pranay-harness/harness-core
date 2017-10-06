package software.wings.service.intfc;

import software.wings.beans.FeatureFlag.Type;

public interface FeatureFlagService { public boolean getFlag(Type type, String accountId); }
