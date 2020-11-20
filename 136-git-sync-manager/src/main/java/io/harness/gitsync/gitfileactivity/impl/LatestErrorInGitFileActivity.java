package io.harness.gitsync.gitfileactivity.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
public class LatestErrorInGitFileActivity {
  @Field("filePath") String filePath;
  @Field("errorMessage") String errorMessage;
}
