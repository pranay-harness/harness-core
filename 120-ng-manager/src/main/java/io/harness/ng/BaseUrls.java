package io.harness.ng;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseUrls {
  String ngManager;
  String ui;
  String ngUi;
}
