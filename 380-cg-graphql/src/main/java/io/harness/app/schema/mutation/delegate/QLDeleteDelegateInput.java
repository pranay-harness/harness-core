package io.harness.app.schema.mutation.delegate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;

import io.harness.annotations.dev.TargetModule;
import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLDeleteDelegateInput implements QLMutationInput {
  String clientMutationId;

  String accountId;
  String delegateId;
  boolean forceDelete;

  @Override
  public String getClientMutationId() {
    return clientMutationId;
  }
}
