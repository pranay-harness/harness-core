package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.ng.core.Resource;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;
import io.harness.scope.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AuditEventDTO {
  @NotNull @NotBlank String insertId;
  @Valid @NotNull ResourceScope resourceScope;

  HttpRequestInfo httpRequestInfo;
  RequestMetadata requestMetadata;

  @NotNull Long timestamp;

  @NotNull @Valid AuthenticationInfo authenticationInfo;

  @NotNull ModuleType module;
  String environmentIdentifier;

  @NotNull @Valid Resource resource;
  @NotNull Action action;

  YamlDiff yamlDiff;
  @Valid AuditEventData auditEventData;

  List<KeyValuePair> additionalInfo;
}
