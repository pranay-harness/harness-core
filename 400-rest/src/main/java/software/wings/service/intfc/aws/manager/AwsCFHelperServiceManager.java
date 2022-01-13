/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

import java.util.List;

@OwnedBy(CDP)
public interface AwsCFHelperServiceManager {
  List<AwsCFTemplateParamsData> getParamsData(String type, String data, String awsConfigId, String region, String appId,
      String scmSettingId, String sourceRepoBranch, String templatePath, String commitId, Boolean useBranch,
      String repoName);
}
