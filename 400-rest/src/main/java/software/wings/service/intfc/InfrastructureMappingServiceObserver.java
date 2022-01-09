/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import software.wings.beans.InfrastructureMapping;

public interface InfrastructureMappingServiceObserver {
  void onSaved(InfrastructureMapping infrastructureMapping);
  void onUpdated(InfrastructureMapping infrastructureMapping);
}
