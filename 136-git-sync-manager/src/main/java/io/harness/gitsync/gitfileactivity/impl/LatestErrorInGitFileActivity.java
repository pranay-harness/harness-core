package io.harness.gitsync.gitfileactivity.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@OwnedBy(DX)
public class LatestErrorInGitFileActivity {
  @Field("filePath") String filePath;
  @Field("errorMessage") String errorMessage;
}
