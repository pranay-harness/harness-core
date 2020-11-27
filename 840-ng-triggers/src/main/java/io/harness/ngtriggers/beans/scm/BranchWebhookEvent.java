package io.harness.ngtriggers.beans.scm;

import static io.harness.ngtriggers.beans.scm.WebhookEvent.Type.BRANCH;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Branch")
public class BranchWebhookEvent implements WebhookEvent {
  private String branchName;
  private String link;
  private List<CommitDetails> commitDetailsList;
  private Repository repository;
  private WebhookBaseAttributes baseAttributes;

  @Override
  public Type getType() {
    return BRANCH;
  }
}
