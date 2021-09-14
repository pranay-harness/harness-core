/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.gitSync;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 2019-08-08.
 */
@Data
@Builder
public class GitWebhookRequestAttributes {
  private String webhookBody;
  private String webhookHeaders;
  @NotEmpty private String branchName;
  private String repositoryFullName;
  @NotEmpty private String gitConnectorId;
  String headCommitId;
}
