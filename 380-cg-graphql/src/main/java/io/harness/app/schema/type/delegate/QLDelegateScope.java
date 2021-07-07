package io.harness.app.schema.type.delegate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLTaskGroup;

@Value
@Builder
@AllArgsConstructor
public class QLDelegateScope implements QLObject {
    String accountId;
    String name;
    String uuid;
    QLEnvironmentType environmentTypes;
    QLTaskGroup taskGroups;
    String environment;
    String applications;
    String services;
    String infrastructureDefinitions;
    String serviceInfrastructures;
}
