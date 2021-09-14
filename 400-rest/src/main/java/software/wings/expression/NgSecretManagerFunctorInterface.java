/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.expression;

// This functor is only to assure compatability between all  SecretManagerFunctors
public interface NgSecretManagerFunctorInterface {
  String FUNCTOR_NAME = "ngSecretManager";
  Object obtain(String secretIdentifier, int token);
}
