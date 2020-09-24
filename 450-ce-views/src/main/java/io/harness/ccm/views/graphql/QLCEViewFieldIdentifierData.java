package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewFieldIdentifier;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLCEViewFieldIdentifierData {
  List<QLCEViewField> values;
  ViewFieldIdentifier identifier;
}
