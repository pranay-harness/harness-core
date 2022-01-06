/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.Environment.Builder.anEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentKeys;
import software.wings.graphql.schema.type.QLEnvironmentConnection;
import software.wings.graphql.schema.type.QLTag.QLTagKeys;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import graphql.ExecutionResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class EnvironmentTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private HarnessTagService harnessTagService;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject EnvironmentGenerator environmentGenerator;
  @Inject AccountGenerator accountGenerator;
  private String accountId;

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironment() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    {
      String query = $GQL(/*
{
  environment(environmentId: "%s") {
    id
    name
    description
    type
    createdAt
    createdBy {
      id
    }
  }
}*/ environment.getUuid());

      QLTestObject qlEnvironment = qlExecute(query, environment.getAccountId());
      assertThat(qlEnvironment.get(QLEnvironmentKeys.id)).isEqualTo(environment.getUuid());
      assertThat(qlEnvironment.get(QLEnvironmentKeys.name)).isEqualTo(environment.getName());
      assertThat(qlEnvironment.get(QLEnvironmentKeys.description)).isEqualTo(environment.getDescription());
      assertThat(qlEnvironment.get(QLEnvironmentKeys.createdAt)).isEqualTo(environment.getCreatedAt());
      assertThat(qlEnvironment.sub(QLEnvironmentKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(environment.getCreatedBy().getUuid());
    }

    {
      String query = $GQL(/*
{
  environment(environmentId: "%s") {
    id
    tags {
      name
      value
    }
  }
}*/ environment.getUuid());

      attachTag(environment);
      QLTestObject qlEnvironment = qlExecute(query, environment.getAccountId());
      assertThat(qlEnvironment.get(QLEnvironmentKeys.id)).isEqualTo(environment.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) qlEnvironment.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingEnvironment() {
    String query = $GQL(/*
{
  environment(environmentId: "blah") {
    id
  }
}*/);

    final ExecutionResult result = qlResult(query, getAccountId());
    assertThat(result.getErrors().size()).isEqualTo(1);

    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/environment) : Entity with id: blah is not found");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironments() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Environments").build());

    final Builder builder = anEnvironment().appId(application.getUuid());

    final Environment environment1 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.uuid(generateUuid()).name("Environment - " + generateUuid()).build());
    final Environment environment2 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.uuid(generateUuid()).name("Environment - " + generateUuid()).build());
    final Environment environment3 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.uuid(generateUuid()).name("Environment - " + generateUuid()).build());

    {
      String query = $GQL(/*
{
  environments(filters:[{application:{operator:EQUALS,values:["%s"]}}]  limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLEnvironmentConnection environmentConnection =
          qlExecute(QLEnvironmentConnection.class, query, application.getAccountId());
      assertThat(environmentConnection.getNodes().size()).isEqualTo(2);

      assertThat(environmentConnection.getNodes().get(0).getId()).isEqualTo(environment3.getUuid());
      assertThat(environmentConnection.getNodes().get(1).getId()).isEqualTo(environment2.getUuid());
    }

    {
      String query = $GQL(/*
{
  environments(filters:[{application:{operator:EQUALS,values:["%s"]}}]  limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLEnvironmentConnection environmentConnection =
          qlExecute(QLEnvironmentConnection.class, query, application.getAccountId());
      assertThat(environmentConnection.getNodes().size()).isEqualTo(2);

      assertThat(environmentConnection.getNodes().get(0).getId()).isEqualTo(environment2.getUuid());
      assertThat(environmentConnection.getNodes().get(1).getId()).isEqualTo(environment1.getUuid());
    }

    {
      String query = $GQL(/*
{
  environments(filters:[{environment:{operator:IN,values:["%s"]}}] limit: 1) {
    nodes {
      id
      tags {
        name
        value
      }
    }
  }
}*/ environment1.getUuid());

      attachTag(environment1);
      QLTestObject environmentConnection = qlExecute(query, application.getAccountId());
      Map<String, Object> envMap = (LinkedHashMap) (((ArrayList) environmentConnection.get("nodes")).get(0));
      assertThat(envMap.get(QLEnvironmentKeys.id)).isEqualTo(environment1.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) envMap.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  private void attachTag(Environment environment) {
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(environment.getAccountId())
                                    .appId(environment.getAppId())
                                    .entityId(environment.getUuid())
                                    .entityType(ENVIRONMENT)
                                    .key("color")
                                    .value("red")
                                    .build());
  }
}
