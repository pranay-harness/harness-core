package io.harness.cdng.artifact.service;

import io.harness.cdng.artifact.bean.Artifact;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import javax.validation.Valid;

/**
 * Entries are immutable, thus no update function should be there.
 */
public interface ArtifactService {
  /**
   * Create/Save Artifact into the collection.
   * @param artifact
   * @return Artifact
   */
  @ValidationGroups(Create.class) Artifact create(@Valid Artifact artifact);

  /**
   * Returns Artifact from collection.
   * @param accountId
   * @param uuid
   * @return Artifact.
   */
  Artifact get(String accountId, String uuid);
}
