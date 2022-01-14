package io.harness.ng.core.switchaccount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SamlIdentificationInfo {
  String origin;
  String metaDataFile;
}
