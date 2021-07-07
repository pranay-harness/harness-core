package software.wings.graphql.schema.type.delegate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLTaskGroup;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class QLDelegateScope implements QLObject {
    String accountId;
    String name;
    List<QLEnvironmentType> environmentTypes;
    List<QLTaskGroup> taskGroups;
    List<String> environment;
    List<String> applications;
    List<String> services;
    List<String> infrastructureDefinitions;
    List<String> serviceInfrastructures;

}
