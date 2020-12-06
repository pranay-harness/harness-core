package software.wings.graphql.schema.mutation;

import software.wings.graphql.schema.type.QLObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface QLMutationInput extends ClientMutationIdAccess, QLObject {}
