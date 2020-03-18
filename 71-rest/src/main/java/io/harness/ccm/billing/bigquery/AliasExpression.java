package io.harness.ccm.billing.bigquery;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.ValidationContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

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
