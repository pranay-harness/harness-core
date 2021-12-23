package io.harness.app.schema.mutation.delegate.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;

import static io.harness.annotations.dev.HarnessTeam.DEL;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
public class QLAttachAllScopesToDelegateInput implements QLMutationInput {
    String clientMutationId;
    String accountId;
    String delegateId;

}
