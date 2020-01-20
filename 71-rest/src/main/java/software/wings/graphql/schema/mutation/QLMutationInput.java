package software.wings.graphql.schema.mutation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.graphql.schema.type.QLObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface QLMutationInput extends RequestIdAccess, QLObject {}
