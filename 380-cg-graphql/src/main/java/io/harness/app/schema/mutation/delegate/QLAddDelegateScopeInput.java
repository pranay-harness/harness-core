package software.wings.graphql.schema.mutation.delegate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.QLInfrastructureDefinitionFilter;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.delegate.QLDelegateScope;

import java.util.List;

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
