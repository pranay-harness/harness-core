package io.harness.ccm.billing.bigquery;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.ValidationContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class TruncExpression extends Expression {
  public enum DatePart { MONTH, DAY, HOUR }

  private final SqlObject _timestampExpression;
  private final Object _datePart;
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
