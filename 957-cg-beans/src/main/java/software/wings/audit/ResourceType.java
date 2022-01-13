/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.audit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public enum ResourceType {
  CLOUD_PROVIDER,
  ARTIFACT_SERVER,
  COLLABORATION_PROVIDER,
  VERIFICATION_PROVIDER,
  SOURCE_REPO_PROVIDER,
  LOAD_BALANCER,
  CONNECTION_ATTRIBUTES,
  SETTING,
  APPLICATION,
  SERVICE,
  ENVIRONMENT,
  WORKFLOW,
  PIPELINE,
  ROLE,
  ENCRYPTED_RECORDS,
  PROVISIONER,
  TRIGGER,
  TEMPLATE,
  TEMPLATE_FOLDER,
  USER_GROUP,
  DEPLOYMENT_FREEZE,
  TAG,
  CUSTOM_DASHBOARD,
  SECRET_MANAGER,
  SSO_SETTINGS,
  USER,
  USER_INVITE,
  DELEGATE,
  DELEGATE_SCOPE,
  DELEGATE_PROFILE,
  API_KEY,
  WHITELISTED_IP,
  CE_CONNECTOR
}
