package io.harness.walktree.beans;

import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DummyVisitorField implements VisitorFieldWrapper {
  public static final VisitorFieldType VISITOR_FIELD_TYPE = VisitorFieldType.builder().type("DUMMY_FIELD").build();

  String value;
  @Override
  public VisitorFieldType getVisitorFieldType() {
    return VisitorFieldType.builder().type("DUMMY").build();
  }
}
