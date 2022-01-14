package io.harness.walktree.visitor.mergeinputset.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MergeVisitorInputSetElement {
  String inputSetIdentifier;
  Object inputSetElement;
}
