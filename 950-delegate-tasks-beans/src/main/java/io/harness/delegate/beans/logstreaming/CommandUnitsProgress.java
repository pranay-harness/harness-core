package io.harness.delegate.beans.logstreaming;

import io.harness.delegate.beans.DelegateProgressData;

import java.util.LinkedHashMap;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandUnitsProgress implements DelegateProgressData {
  @Builder.Default LinkedHashMap<String, CommandUnitProgress> commandUnitProgressMap = new LinkedHashMap<>();
}
