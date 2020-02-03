package software.wings.service.impl.yaml;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class YamlProcessingLogContext extends AutoLogContext {
  public static final String GIT_CONNECTOR_ID = "gitConnectorId";
  public static final String BRANCH_NAME = "branchName";
  public static final String WEBHOOK_TOKEN = "webhookToken";
  public YamlProcessingLogContext(ImmutableMap<String, String> context, OverrideBehavior behavior) {
    super(context, behavior);
  }
}
