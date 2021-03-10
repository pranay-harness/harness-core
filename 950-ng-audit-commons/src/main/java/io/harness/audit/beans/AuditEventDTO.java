package io.harness.audit.beans;

import io.harness.ModuleType;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.Resource;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AuditEventDTO {
  @NotNull @NotBlank String id;
  @NotNull @NotBlank String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;

  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;

  Long timestamp;

  @NotNull @Valid AuthenticationInfo authenticationInfo;

  @NotNull ModuleType moduleType;

  @NotNull @Valid Resource resource;
  @NotNull @NotBlank String action;

  YamlDiff yamlDiff;
  @Valid AuditEventDataDTO auditEventData;

  Map<String, String> additionalInfo;
}
