package io.harness.overlayinputset;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OverlayInputSet implements OverlayInputSetWrapper {
  String identifier;
  String name;
  String description;
  List<String> inputSetsReferences;

  // add tags
}
