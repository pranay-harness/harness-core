package io.harness.app.schema.mutation.delegate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import io.harness.app.schema.type.delegate.QLDelegateScope;

import static io.harness.annotations.dev.HarnessTeam.PL;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAddDelegateScopeInput implements QLMutationInput {
   String clientMutationId;
   QLDelegateScope delegateScope;

}
