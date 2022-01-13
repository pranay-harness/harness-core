/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.graphql;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.ValidationContext;
import java.io.IOException;

public class TimestampMillisExpression extends Expression {
  private final SqlObject timestampExpression;

  public TimestampMillisExpression(Object timestampExpression) {
    this.timestampExpression = Converter.toColumnSqlObject(timestampExpression);
  }

  @Override
  public boolean hasParens() {
    return false;
  }

  @Override
  protected void collectSchemaObjects(ValidationContext vContext) {
    SqlObject.collectSchemaObjects(timestampExpression, vContext);
  }

  @Override
  public void appendTo(AppendableExt app) throws IOException {
    app.append("TIMESTAMP_MILLIS(").append(timestampExpression).append(")");
  }
}
