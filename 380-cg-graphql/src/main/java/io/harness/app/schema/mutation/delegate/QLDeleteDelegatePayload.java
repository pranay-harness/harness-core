package io.harness.app.schema.mutation.delegate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;

import io.harness.annotations.dev.TargetModule;
import software.wings.graphql.schema.mutation.QLMutationPayload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLDeleteDelegatePayload implements QLMutationPayload {
  String clientMutationId;
  String message;

  @Override
  public String getClientMutationId() {
    return clientMutationId;
  }
}
