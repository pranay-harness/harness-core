/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.template.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.EntityReferenceRequest;
import io.harness.pms.contracts.service.EntityReferenceResponse;
import io.harness.pms.contracts.service.EntityReferenceServiceGrpc.EntityReferenceServiceBlockingStub;
import io.harness.template.entity.TemplateEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Singleton
public class TemplateReferenceHelper {
  EntityReferenceServiceBlockingStub entityReferenceServiceBlockingStub;
  TemplateYamlConversionHelper templateYamlConversionHelper;

  public void populateTemplateReferences(TemplateEntity templateEntity) {
    String pmsUnderstandableYaml =
        templateYamlConversionHelper.convertTemplateYamlToPMSUnderstandableYaml(templateEntity);
    EntityReferenceResponse response = entityReferenceServiceBlockingStub.getReferences(
        EntityReferenceRequest.newBuilder().setYaml(pmsUnderstandableYaml).build());
  }
}
