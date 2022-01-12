/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.elk;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Data;

/**
 * Created by rsingh on 8/23/17.
 */
@Data
@OwnedBy(HarnessTeam.CV)
public class ElkIndexTemplate {
  private String name;
  private Map<String, Object> properties;
}
