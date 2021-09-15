/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.ci;

/**
 * Provides Manager Auth secret key to CI
 */

public interface CIServiceAuthSecretKey {
  String getCIAuthServiceSecretKey();
}
