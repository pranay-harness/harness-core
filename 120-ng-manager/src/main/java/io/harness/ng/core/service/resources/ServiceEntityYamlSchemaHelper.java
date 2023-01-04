/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.service.resources;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorDTO;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorWrapperDTO;
import io.harness.utils.YamlPipelineUtils;
import io.harness.utils.featureflaghelper.NGFeatureFlagHelperService;
import io.harness.yaml.validator.InvalidYamlException;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.networknt.schema.ValidationMessage;
import java.util.Collections;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Singleton
@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class ServiceEntityYamlSchemaHelper {
  private NGFeatureFlagHelperService featureFlagHelperService;
  private YamlSchemaValidator yamlSchemaValidator;

  void validateSchema(String accountId, String yaml) {
    if (featureFlagHelperService.isEnabled(accountId, FeatureName.NG_SVC_ENV_REDESIGN)
        && featureFlagHelperService.isEnabled(accountId, FeatureName.CDS_SERVICE_ENV_SCHEMA_VALIDATION)
        && isNotEmpty(yaml)) {
      long start = System.currentTimeMillis();
      try {
        JsonNode yamlNode = YamlPipelineUtils.getMapper().readTree(yaml);
        Set<ValidationMessage> validationMessages =
            yamlSchemaValidator.validateWithDetailedMessage(yaml, EntityType.SERVICE);
        yamlSchemaValidator.processAndHandleValidationMessage(yamlNode, validationMessages, yaml);
      } catch (InvalidYamlException e) {
        throw e;
      } catch (Exception ex) {
        log.error(ex.getMessage(), ex);
        YamlSchemaErrorWrapperDTO errorWrapperDTO =
            YamlSchemaErrorWrapperDTO.builder()
                .schemaErrors(Collections.singletonList(
                    YamlSchemaErrorDTO.builder().message(ex.getMessage()).fqn("$.service").build()))
                .build();
        throw new InvalidYamlException(ex.getMessage(), ex, errorWrapperDTO, yaml);
      } finally {
        log.info("[NG_MANAGER] Schema validation took total time {}ms", System.currentTimeMillis() - start);
      }
    }
  }
}
