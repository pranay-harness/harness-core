/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static java.lang.String.format;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.ServiceVariableKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.service.YamlHelper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class AmendCorruptedEncryptedServiceVariable implements Migration {
  @Inject private YamlHelper yamlHelper;
  @Inject private WingsPersistence wingsPersistence;

  private static final String DEBUG_LINE = "AMEND_SERVICE_VARIABLES: ";

  @Override
  public void migrate() {
    int updations = 0;
    log.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE, "Starting amending corrupted service variables"));
    try (HIterator<ServiceVariable> serviceVariableHIterator =
             new HIterator<>(wingsPersistence.createQuery(ServiceVariable.class)
                                 .field(ServiceVariableKeys.accountId)
                                 .exists()
                                 .field(ServiceVariableKeys.encryptedValue)
                                 .contains(":")
                                 .fetch())) {
      for (ServiceVariable serviceVariable : serviceVariableHIterator) {
        /*
         encrypted values in hashicorp vault are stored differently from others. Their yaml
         reference does not have the same format
         */
        if (serviceVariable.getEncryptedValue() != null && serviceVariable.getEncryptedValue().contains(":")
            && !serviceVariable.getEncryptedValue().startsWith("hashicorpvault:")) {
          String corruptedValue = serviceVariable.getEncryptedValue();
          String correctValue = yamlHelper.extractEncryptedRecordId(corruptedValue, serviceVariable.getAccountId());
          log.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE,
              format("Updating Service Variable from %s-> %s", corruptedValue, correctValue)));
          serviceVariable.setEncryptedValue(correctValue);
          wingsPersistence.save(serviceVariable);
          updations++;
        }
      }
    }
    log.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE,
        "Finished amending corrupted "
            + "service variables total:",
        String.valueOf(updations)));
  }
}
