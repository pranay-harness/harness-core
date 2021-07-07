package io.harness.app.schema.mutation.delegate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import io.harness.app.schema.type.delegate.QLDelegateScope;

import static io.harness.annotations.dev.HarnessTeam.PL;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAddDelegateScopePayload implements QLMutationPayload {
    String clientMutationId;
    String message;
    QLDelegateScope qlDelegateScope;
}
