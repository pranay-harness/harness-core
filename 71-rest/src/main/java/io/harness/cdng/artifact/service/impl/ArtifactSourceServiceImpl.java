package io.harness.cdng.artifact.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.repository.ArtifactRepository;
import io.harness.cdng.artifact.repository.ArtifactSourceDao;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ArtifactSourceServiceImpl implements ArtifactSourceService {
  @Inject private ArtifactSourceDao artifactSourceDao;
  @Inject private ArtifactRepository artifactRepository;

  @Override
  @ValidationGroups(Create.class)
  public ArtifactSource create(ArtifactSource artifactSource) {
    return create(artifactSource, true);
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactSource create(ArtifactSource artifactSource, boolean validate) {
    // TODO(archit): use usageRestrictions later.
    // TODO(archit): Validate artifact source.
    return artifactSourceDao.create(artifactSource);
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactSource saveOrGetArtifactStream(ArtifactSource artifactSource) {
    ArtifactSource existingArtifactSource =
        getArtifactStreamByHash(artifactSource.getAccountId(), artifactSource.getUniqueHash());
    if (existingArtifactSource == null) {
      return create(artifactSource);
    }
    return existingArtifactSource;
  }

  @Override
  public ArtifactSource get(String accountId, String uuid) {
    return artifactSourceDao.get(accountId, uuid);
  }

  @Override
  public ArtifactSource getArtifactStreamByHash(String accountId, String uniqueHash) {
    return artifactSourceDao.getArtifactStreamByHash(accountId, uniqueHash);
    // return artifactSourceDao.
  }
}
