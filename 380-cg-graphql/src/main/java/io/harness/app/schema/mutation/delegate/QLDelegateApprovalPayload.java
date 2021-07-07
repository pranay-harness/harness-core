package io.harness.app.schema.mutation.delegate;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import io.harness.app.schema.type.delegate.QLDelegate;

import static io.harness.annotations.dev.HarnessTeam.PL;


@Value
@Builder
@AllArgsConstructor
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLDelegateApprovalPayload implements QLMutationPayload {

    String clientMutationId;
    QLDelegate delegate;

    @Override
    public String getClientMutationId() {
        return clientMutationId;
    }
}
