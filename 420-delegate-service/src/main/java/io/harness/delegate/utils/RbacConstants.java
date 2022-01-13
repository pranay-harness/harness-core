/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

public interface RbacConstants {
  String DELEGATE_RESOURCE_TYPE = "DELEGATE";
  String DELEGATE_VIEW_PERMISSION = "core_delegate_view";
  String DELEGATE_EDIT_PERMISSION = "core_delegate_edit";
  String DELEGATE_DELETE_PERMISSION = "core_delegate_delete";

  String DELEGATE_CONFIG_RESOURCE_TYPE = "DELEGATECONFIGURATION";
  String DELEGATE_CONFIG_VIEW_PERMISSION = "core_delegateconfiguration_view";
  String DELEGATE_CONFIG_EDIT_PERMISSION = "core_delegateconfiguration_edit";
  String DELEGATE_CONFIG_DELETE_PERMISSION = "core_delegateconfiguration_delete";
}
