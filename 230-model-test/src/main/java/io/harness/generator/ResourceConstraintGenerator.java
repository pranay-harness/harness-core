/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.beans.ResourceConstraint.builder;
import static io.harness.govern.Switch.unhandled;

import io.harness.beans.ResourceConstraint;
import io.harness.beans.ResourceConstraint.ResourceConstraintBuilder;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.generator.AccountGenerator.Accounts;
import io.harness.generator.OwnerManager.Owners;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ResourceConstraintService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;

@Singleton
public class ResourceConstraintGenerator {
  @Inject private OwnerManager ownerManager;
  @Inject AccountGenerator accountGenerator;

  @Inject ResourceConstraintService resourceConstraintService;
  @Inject WingsPersistence wingsPersistence;

  public enum ResourceConstraints { GENERIC_ASAP_TEST, GENERIC_FIFO_TEST }

  public ResourceConstraint ensurePredefined(Randomizer.Seed seed, Owners owners, ResourceConstraints predefined) {
    switch (predefined) {
      case GENERIC_ASAP_TEST:
        return ensureGenericAsapTest(seed, owners);
      case GENERIC_FIFO_TEST:
        return ensureGenericFifoTest(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private ResourceConstraint ensureGenericAsapTest(Randomizer.Seed seed, Owners owners) {
    owners.obtainAccount(() -> accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST));
    return ensureResourceConstraint(
        seed, owners, builder().name("Test ASAP constraint").capacity(10).strategy(Strategy.ASAP).build());
  }

  private ResourceConstraint ensureGenericFifoTest(Randomizer.Seed seed, Owners owners) {
    owners.obtainAccount(() -> accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST));
    return ensureResourceConstraint(
        seed, owners, builder().name("Test FIFO constraint").capacity(10).strategy(Strategy.FIFO).build());
  }

  public ResourceConstraint ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    ResourceConstraints predefined = random.nextObject(ResourceConstraints.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public ResourceConstraint exists(ResourceConstraint resourceConstraint) {
    return wingsPersistence.createQuery(ResourceConstraint.class)
        .filter(ResourceConstraint.ACCOUNT_ID_KEY, resourceConstraint.getAccountId())
        .filter(ResourceConstraint.NAME_KEY, resourceConstraint.getName())
        .get();
  }

  public ResourceConstraint ensureResourceConstraint(
      Randomizer.Seed seed, Owners owners, ResourceConstraint resourceConstraint) {
    EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                                .seed(seed.getValue())
                                .randomize(Integer.class, new IntegerRangeRandomizer(10, 100))
                                .build();

    ResourceConstraintBuilder builder = ResourceConstraint.builder();

    if (resourceConstraint != null && resourceConstraint.getAccountId() != null) {
      builder.accountId(resourceConstraint.getAccountId());
    } else {
      Account account = owners.obtainAccount(() -> accountGenerator.ensureRandom(seed, owners));
      builder.accountId(account.getUuid());
    }

    if (resourceConstraint != null && resourceConstraint.getName() != null) {
      builder.name(resourceConstraint.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    ResourceConstraint existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (resourceConstraint != null && resourceConstraint.getCapacity() != 0) {
      builder.capacity(resourceConstraint.getCapacity());
    } else {
      builder.capacity(random.nextObject(Integer.class));
    }

    if (resourceConstraint != null && resourceConstraint.getStrategy() != null) {
      builder.strategy(resourceConstraint.getStrategy());
    } else {
      builder.strategy(random.nextObject(Strategy.class));
    }

    final ResourceConstraint finalResourceConstraint = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> resourceConstraintService.save(finalResourceConstraint), () -> exists(finalResourceConstraint));
  }
}
