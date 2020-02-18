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
  @NotEmpty private String gitConnectorId;
  String headCommitId;
}
