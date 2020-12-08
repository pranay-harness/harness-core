package io.harness.ng.core.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.GCP_KMS;
import static io.harness.delegate.beans.connector.ConnectorType.LOCAL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.exception.WingsException;
import io.harness.secretmanagerclient.dto.GcpKmsConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptionType;

import io.dropwizard.jersey.validation.JerseyViolationException;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@UtilityClass
@OwnedBy(PL)
public class NGUtils {
  private static final Validator validator = Validation.buildDefaultValidatorFactory().usingContext().getValidator();

  public static void verifyValuesNotChanged(List<Pair<?, ?>> valuesList) {
    for (Pair<?, ?> pair : valuesList) {
      if (!pair.getKey().equals(pair.getValue())) {
        throw new InvalidRequestException(
            "Value mismatch, previous: " + pair.getKey() + " current: " + pair.getValue());
      }
    }
  }

  public static void validate(Object entity) {
    Set<ConstraintViolation<Object>> constraints = validator.validate(entity);
    if (isNotEmpty(constraints)) {
      throw new JerseyViolationException(constraints, null);
    }
  }

  public static void verifyValuesNotChanged(List<Pair<?, ?>> valuesList, boolean present) {
    if (present) {
      for (Pair<?, ?> pair : valuesList) {
        if (pair.getValue() != null && (pair.getKey() == null || !pair.getKey().equals(pair.getValue()))) {
          throw new InvalidRequestException(
              "Value mismatch, previous: " + pair.getKey() + " current: " + pair.getValue());
        }
      }
    } else {
      for (Pair<?, ?> pair : valuesList) {
        if (!pair.getKey().equals(pair.getValue())) {
          throw new InvalidRequestException(
              "Value mismatch, previous: " + pair.getKey() + " current: " + pair.getValue());
        }
      }
    }
  }

  public static <T> Page<T> getPaginatedResult(
      Criteria criteria, Pageable pageable, Class<T> clazz, MongoTemplate mongoTemplate) {
    Query query = new Query(criteria).with(pageable);
    List<T> objects = mongoTemplate.find(query, clazz);
    return PageableExecutionUtils.getPage(
        objects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1L), clazz));
  }

  public static ConnectorDTO getConnectorRequestDTO(SecretManagerConfigDTO secretManagerConfigDTO) {
    ConnectorInfoDTO connectorInfo;
    switch (secretManagerConfigDTO.getEncryptionType()) {
      case GCP_KMS:
        GcpKmsConfigDTO gcpKmsConfig = (GcpKmsConfigDTO) secretManagerConfigDTO;
        GcpKmsConnectorDTO gcpKmsConnectorDTO = GcpKmsConnectorDTO.builder()
                                                    .region(gcpKmsConfig.getRegion())
                                                    .keyRing(gcpKmsConfig.getKeyRing())
                                                    .keyName(gcpKmsConfig.getKeyName())
                                                    .projectId(gcpKmsConfig.getProjectId())
                                                    .credentials(gcpKmsConfig.getCredentials())
                                                    .isDefault(secretManagerConfigDTO.isDefault())
                                                    .build();
        connectorInfo = ConnectorInfoDTO.builder()
                            .connectorType(GCP_KMS)
                            .identifier(secretManagerConfigDTO.getIdentifier())
                            .name(secretManagerConfigDTO.getName())
                            .orgIdentifier(secretManagerConfigDTO.getOrgIdentifier())
                            .projectIdentifier(secretManagerConfigDTO.getProjectIdentifier())
                            .description(secretManagerConfigDTO.getDescription())
                            .connectorConfig(gcpKmsConnectorDTO)
                            .build();
        break;
      case LOCAL:
        LocalConnectorDTO localConnectorDTO =
            LocalConnectorDTO.builder().isDefault(secretManagerConfigDTO.isDefault()).build();
        connectorInfo = ConnectorInfoDTO.builder()
                            .connectorType(LOCAL)
                            .identifier(secretManagerConfigDTO.getIdentifier())
                            .name(secretManagerConfigDTO.getName())
                            .orgIdentifier(secretManagerConfigDTO.getOrgIdentifier())
                            .projectIdentifier(secretManagerConfigDTO.getProjectIdentifier())
                            .description(secretManagerConfigDTO.getDescription())
                            .connectorConfig(localConnectorDTO)
                            .build();
        break;
      default:
        throw new SecretManagementException(
            ErrorCode.SECRET_MANAGEMENT_ERROR, "Unsupported Secret Manager", WingsException.USER);
    }
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  public static String getDefaultHarnessSecretManagerName(EncryptionType encryptionType) {
    switch (encryptionType) {
      case GCP_KMS:
        return "Harness Secrets Manager - Google KMS";
      case LOCAL:
        return "Harness Vault";
      default:
        throw new SecretManagementException(
            ErrorCode.SECRET_MANAGEMENT_ERROR, "Unsupported Harness Secret Manager", WingsException.USER);
    }
  }
}
