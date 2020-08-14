package io.harness.delegate.git;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;

public interface NGGitService { String validate(GitConfigDTO gitConfig, String accountId); }
