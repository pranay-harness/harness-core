/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
public class ConstExpression extends Expression {
  private final String _constant;
  @Getter public final String alias;

  public ConstExpression(String constant, String alias) {
    this._constant = constant;
    this.alias = alias;
  }

  @Override
  protected void collectSchemaObjects(ValidationContext vContext) {
    SqlObject.collectSchemaObjects(Converter.toColumnSqlObject(_constant), vContext);
  }

  @Override
  public void appendTo(AppendableExt app) throws IOException {
    app.append('\'').append(_constant).append('\'');
    if (StringUtils.isNotBlank(alias)) {
      app.append(" AS ").append(alias);
    }
  }
}
