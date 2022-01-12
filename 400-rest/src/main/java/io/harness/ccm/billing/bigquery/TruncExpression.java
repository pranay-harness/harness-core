/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.bigquery;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.ValidationContext;
import java.io.IOException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CE)
public class TruncExpression extends Expression {
  public enum DatePart { MONTH, DAY, HOUR, WEEK }

  private final SqlObject _timestampExpression;
  @Getter public final Object _datePart;
  @Getter public final String alias;

  public TruncExpression(Object timestampExpression, DatePart datePart) {
    this(timestampExpression, (Object) datePart, null);
  }

  public TruncExpression(Object timestampExpression, Object datePart, String alias) {
    _timestampExpression = Converter.toColumnSqlObject(timestampExpression);
    _datePart = datePart;
    this.alias = alias;
  }

  @Override
  public boolean hasParens() {
    return false;
  }

  @Override
  protected void collectSchemaObjects(ValidationContext vContext) {
    SqlObject.collectSchemaObjects(_timestampExpression, vContext);
  }

  @Override
  public void appendTo(AppendableExt app) throws IOException {
    app.append("TIMESTAMP_TRUNC(").append(_timestampExpression).append(",").append(_datePart).append(")");
    if (StringUtils.isNotBlank(alias)) {
      app.append(" AS ").append(alias);
    }
  }
}
