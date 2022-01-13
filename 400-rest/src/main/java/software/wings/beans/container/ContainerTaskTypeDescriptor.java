/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.container;

import software.wings.stencils.Stencil;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by anubhaw on 2/6/17.
 */
public interface ContainerTaskTypeDescriptor extends Stencil<ContainerTask> {
  @Override @JsonIgnore Class<? extends ContainerTask> getTypeClass();
}
