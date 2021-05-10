package io.harness.file;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.DataStorageMode;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class FileServiceConfiguration {
  @Builder.Default DataStorageMode fileStorageMode = DataStorageMode.MONGO;
  @Builder.Default String clusterName = "";
}
