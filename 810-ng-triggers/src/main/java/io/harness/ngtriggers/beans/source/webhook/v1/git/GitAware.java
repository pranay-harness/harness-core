package io.harness.ngtriggers.beans.source.webhook.v1.git;

public interface GitAware {
  String getConnectorRef();
  String getRepoName();
}
