package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ci.CIBuildAuthorProto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CI)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIBuildAuthor {
  private String id;
  private String name;
  private String email;
  private String avatar;

  public CIBuildAuthorProto toProto() {
    CIBuildAuthorProto.Builder builder = CIBuildAuthorProto.newBuilder();
    if (id != null) {
      builder.setId(id);
    }
    if (name != null) {
      builder.setName(name);
    }
    if (email != null) {
      builder.setEmail(email);
    }
    if (avatar != null) {
      builder.setAvatar(avatar);
    }
    return builder.build();
  }
}
