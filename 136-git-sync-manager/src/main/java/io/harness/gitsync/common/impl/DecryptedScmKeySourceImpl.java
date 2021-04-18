package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.service.DecryptedScmKeySource;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.tasks.DecryptGitApiAccessHelper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;
import org.checkerframework.checker.nullness.qual.NonNull;

@ParametersAreNonnullByDefault
@Singleton
@OwnedBy(DX)
public class DecryptedScmKeySourceImpl implements DecryptedScmKeySource {
  HarnessToGitHelperService harnessToGitHelperService;
  ConnectorService connectorService;
  DecryptGitApiAccessHelper decryptScmApiAccess;
  private final @NonNull Cache<IdentifierRef, Object> keyCache;
  private final int DECRYPTED_SCM_CACHE_TIME = 30 /*minutes*/;
  private final int DECRYPTED_SCM_CACHE_SIZE = 1000;

  @Inject
  public DecryptedScmKeySourceImpl(HarnessToGitHelperService harnessToGitHelperService,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptGitApiAccessHelper) {
    this.keyCache = Caffeine.newBuilder()
                        .maximumSize(DECRYPTED_SCM_CACHE_SIZE)
                        .expireAfterWrite(DECRYPTED_SCM_CACHE_TIME, TimeUnit.MINUTES)
                        .build();
    this.harnessToGitHelperService = harnessToGitHelperService;
    this.connectorService = connectorService;
    this.decryptScmApiAccess = decryptGitApiAccessHelper;
  }

  @Override
  public ScmConnector fetchKey(IdentifierRef scmConnectorRef) {
    return (ScmConnector) keyCache.get(scmConnectorRef, ref -> getDecryptedScmConnector(scmConnectorRef));
  }

  @Override
  public void removeKey(IdentifierRef scmConnectorRef) {
    keyCache.invalidate(scmConnectorRef);
  }

  public ScmConnector getDecryptedScmConnector(IdentifierRef identifierRef) {
    final Optional<ConnectorResponseDTO> connectorResponseDTO =
        connectorService.get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
            identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    return connectorResponseDTO
        .map(connector
            -> decryptScmApiAccess.decryptScmApiAccess((ScmConnector) connector.getConnector().getConnectorConfig(),
                identifierRef.getAccountIdentifier(), identifierRef.getProjectIdentifier(),
                identifierRef.getOrgIdentifier()))
        .orElseThrow(()
                         -> new InvalidRequestException(
                             String.format("Connector doesn't exist for %s.", identifierRef.getIdentifier())));
  }
}
