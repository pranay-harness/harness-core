package io.harness.overlayinputset;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OverlayInputSetConfig implements OverlayInputSetWrapper {
  String identifier;
  String name;
  String description;
  List<String> inputSetReferences;

  Map<String, String> tags;
}
