/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionFunctor;
import io.harness.security.SimpleEncryption;

import lombok.Value;

@OwnedBy(CDC)
@Value
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ManagerPreviewExpressionEvaluator extends ExpressionEvaluator {
  private final ExpressionFunctor secretManagerFunctor;
  private final ExpressionFunctor sweepingOutputSecretFunctor;

  public ManagerPreviewExpressionEvaluator() {
    secretManagerFunctor = new SecretManagerPreviewFunctor();
    sweepingOutputSecretFunctor = SweepingOutputSecretFunctor.builder()
                                      .mode(SecretManagerMode.CHECK_FOR_SECRETS)
                                      .simpleEncryption(new SimpleEncryption())
                                      .build();
    addFunctor(SecretManagerFunctorInterface.FUNCTOR_NAME, secretManagerFunctor);
    addFunctor("sweepingOutputSecrets", sweepingOutputSecretFunctor);
  }

  public ManagerPreviewExpressionEvaluator(
      ExpressionFunctor theSecretManagerFunctor, ExpressionFunctor theSweepingOutputSecretFunctor) {
    secretManagerFunctor = theSecretManagerFunctor;
    sweepingOutputSecretFunctor = theSweepingOutputSecretFunctor;
    addFunctor(SecretManagerFunctorInterface.FUNCTOR_NAME, secretManagerFunctor);
    addFunctor("sweepingOutputSecrets", sweepingOutputSecretFunctor);
  }

  public static ManagerPreviewExpressionEvaluator evaluatorWithSecretExpressionFormat() {
    return new ManagerPreviewExpressionEvaluator(SecretManagerPreviewFunctor.functorWithSecretExpressionFormat(),
        SweepingOutputSecretFunctor.builder().mode(SecretManagerMode.DRY_RUN).build());
  }
}
