package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewFieldIdentifier;

import io.leangen.graphql.annotations.GraphQLNonNull;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewFieldIdentifierData {
  @GraphQLNonNull List<QLCEViewField> values;
  @GraphQLNonNull ViewFieldIdentifier identifier;
  @GraphQLNonNull String identifierName;
}
