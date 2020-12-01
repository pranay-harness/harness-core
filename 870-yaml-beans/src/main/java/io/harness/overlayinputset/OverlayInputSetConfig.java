package io.harness.overlayinputset;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverlayInputSetConfig implements OverlayInputSetWrapper {
  String identifier;
  String name;
  String description;
  List<String> inputSetReferences;

  Map<String, String> tags;
}
