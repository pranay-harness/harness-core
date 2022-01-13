/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.infra.InfrastructureDefinition;

import java.util.List;

/**
 * Created by anubhaw on 10/4/16.
 */
public interface InfrastructureProvider {
  PageResponse<Host> listHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req);

  PageResponse<Host> listHosts(InfrastructureDefinition infrastructureDefinition,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req);

  /**
   * Save host host.
   *
   * @param host the host
   * @return the host
   */
  Host saveHost(Host host);

  /**
   * Delete host.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   * @param dnsName        the dns name
   */
  void deleteHost(String appId, String infraMappingId, String dnsName);

  /**
   * Update host conn attrs.
   *
   * @param infrastructureMapping the infrastructure mapping
   * @param hostConnectionAttrs   the host connection attrs
   */
  void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs);
}
