/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.execution.export.request;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;

import com.google.inject.Inject;
import com.mongodb.DBObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

public class ExportExecutionsRequestQueryTest extends WingsBaseTest {
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @SuppressWarnings("deprecation")
  public void testConversion() {
    assertThat(ExportExecutionsRequestQuery.fromQuery(null)).isNull();

    Query<WorkflowExecution> query = persistence.createQuery(WorkflowExecution.class).filter("accountId", "aid");
    ExportExecutionsRequestQuery requestQuery = ExportExecutionsRequestQuery.fromQuery(query);
    assertThat(requestQuery).isNotNull();
    assertThat(requestQuery.getDbObjectJson()).isNotNull();

    Query<WorkflowExecution> newQuery = persistence.createQuery(WorkflowExecution.class);
    assertThatCode(() -> ExportExecutionsRequestQuery.updateQuery(null, requestQuery)).doesNotThrowAnyException();
    assertThatCode(() -> ExportExecutionsRequestQuery.updateQuery(query, null)).doesNotThrowAnyException();

    ExportExecutionsRequestQuery.updateQuery(newQuery, requestQuery);
    DBObject dbObject = newQuery.getQueryObject();
    assertThat(dbObject).isNotNull();
  }
}
