package io.harness.cdng.artifact.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.Artifact;
import io.harness.cdng.artifact.service.ArtifactDao;
import io.harness.cdng.artifact.service.ArtifactService;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ArtifactServiceImpl implements ArtifactService {
  @Inject private ArtifactDao artifactDao;

  @Override
  @ValidationGroups(Create.class)
  public Artifact create(Artifact artifact) {
    return artifactDao.create(artifact);
  }

  @Override
  public Artifact get(String accountId, String uuid) {
    return artifactDao.get(accountId, uuid);
  }
}
