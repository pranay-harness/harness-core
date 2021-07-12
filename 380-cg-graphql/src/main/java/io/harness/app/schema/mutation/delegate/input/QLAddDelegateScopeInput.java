package io.harness.app.schema.mutation.delegate.input;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.type.delegate.QLTaskGroup;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.QLEnvironmentType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
public class QLAddDelegateScopeInput implements QLMutationInput {
  String clientMutationId;
  String accountId;
  String name;
  List<QLEnvironmentType> environmentTypes;
  List<QLTaskGroup> taskGroups;
  List<String> environments;
  List<String> applications;
  List<String> services;
  List<String> infrastructureDefinitions;
}
