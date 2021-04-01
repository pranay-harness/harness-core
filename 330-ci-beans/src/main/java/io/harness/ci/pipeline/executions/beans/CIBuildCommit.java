package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ci.CIBuildCommitProto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CI)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIBuildCommit {
  private String id;
  private String link;
  private String message;
  private String ownerName;
  private String ownerId;
  private String ownerEmail;
  private long timeStamp;

  public CIBuildCommitProto toProto() {
    CIBuildCommitProto.Builder builder = CIBuildCommitProto.newBuilder();
    if (id != null) {
      builder.setId(id);
    }
    if (link != null) {
      builder.setLink(link);
    }
    if (message != null) {
      builder.setMessage(message);
    }
    if (ownerName != null) {
      builder.setOwnerName(ownerName);
    }
    if (ownerId != null) {
      builder.setOwnerId(ownerId);
    }
    if (ownerEmail != null) {
      builder.setOwnerEmail(ownerEmail);
    }
    builder.setTimeStamp(timeStamp);
    return builder.build();
  }
}
