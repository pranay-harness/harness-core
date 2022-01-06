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
public class AliasExpression extends Expression {
  private final String _constant;
  @Getter public final String alias;

  public AliasExpression(String constant, String alias) {
    this._constant = constant;
    this.alias = alias;
  }

  @Override
  protected void collectSchemaObjects(ValidationContext vContext) {
    SqlObject.collectSchemaObjects(Converter.toColumnSqlObject(_constant), vContext);
  }

  @Override
  public void appendTo(AppendableExt app) throws IOException {
    app.append(_constant);
    if (StringUtils.isNotBlank(alias)) {
      app.append(" AS ").append(alias);
    }
  }
}
