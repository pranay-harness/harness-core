package io.harness.gitsync.persistance;

import io.harness.git.model.ChangeType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import lombok.SneakyThrows;

@Singleton
public class SCMHelper<Y, B extends GitSyncableEntity> {
  @SneakyThrows
  <B> B pushToGit(Y yaml, ChangeType changeType) {
    ObjectMapper objectMapper = new ObjectMapper();
    final String s = pushToGitAndReturnSuccessPayLoad(yaml);
    final Object o = objectMapper.readValue(s, (TypeReference) yaml);
    return null;
  }

  String pushToGitAndReturnSuccessPayLoad(Y yaml) {
    // push to git and lets say return is s
    return "";
  }
}
