package io.harness.ng.core.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.api.repositories.spring.SecretRepository;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.utils.PageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collections;
import java.util.Optional;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGSecretServiceV2Impl implements NGSecretServiceV2 {
  private final SecretRepository secretRepository;

  @Override
  public Optional<Secret> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return secretRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<Secret> secretV2Optional = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretV2Optional.isPresent()) {
      secretRepository.delete(secretV2Optional.get());
      return true;
    }
    return false;
  }

  @Override
  public Secret create(String accountIdentifier, SecretDTOV2 dto) {
    Secret secret = dto.toEntity();
    secret.setAccountIdentifier(accountIdentifier);
    return secretRepository.save(secret);
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    Optional<Secret> secretOptional =
        get(accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), dto.getIdentifier());
    if (secretOptional.isPresent()) {
      Secret oldSecret = secretOptional.get();
      Secret newSecret = dto.toEntity();
      oldSecret.setDescription(newSecret.getDescription());
      oldSecret.setName(newSecret.getName());
      oldSecret.setTags(newSecret.getTags());
      oldSecret.setSecretSpec(newSecret.getSecretSpec());
      oldSecret.setType(newSecret.getType());
      secretRepository.save(oldSecret);
      return true;
    }
    return false;
  }

  @Override
  public SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, SecretValidationMetaData metadata) {
    Optional<Secret> secretV2Optional = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretV2Optional.isPresent() && secretV2Optional.get().getType() == SecretType.SSHKey) {
      // write validation logic here
      return SecretValidationResultDTO.builder().success(true).message(null).build();
    }
    return SecretValidationResultDTO.builder().success(false).message("Validation failed").build();
  }

  @Override
  public Page<Secret> list(Criteria criteria, int page, int size) {
    return secretRepository.findAll(
        criteria, PageUtils.getPageRequest(page, size, Collections.singletonList(SecretKeys.createdAt + ",desc")));
  }
}
