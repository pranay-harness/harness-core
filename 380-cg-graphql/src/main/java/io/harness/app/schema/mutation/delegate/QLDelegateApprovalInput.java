package software.wings.graphql.schema.mutation.delegate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.mutation.delegate.QLDelegateApproval;

import static io.harness.annotations.dev.HarnessTeam.PL;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLDelegateApprovalInput implements QLMutationInput {
    String clientMutationId;

    String delegateId;
    String accountId;
    QLDelegateApproval delegateApproval;



}
