/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.commons.bootstrap;

import io.harness.accesscontrol.commons.bootstrap.ConfigurationState.ConfigurationStateKeys;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ConfigurationStateRepository {
  private final MongoTemplate mongoTemplate;

  @Inject
  public ConfigurationStateRepository(@Named("mongoTemplate") MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public Optional<ConfigurationState> getByIdentifier(@NotEmpty String identifier) {
    Criteria criteria = Criteria.where(ConfigurationStateKeys.identifier).is(identifier);
    return Optional.ofNullable(mongoTemplate.findOne(new Query(criteria), ConfigurationState.class));
  }

  public void upsert(@NotNull ConfigurationState configurationState) {
    mongoTemplate.save(configurationState);
  }
}
