/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.routines;

import io.harness.timescaledb.Public;

import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.Parameter;
import org.jooq.impl.AbstractRoutine;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.types.YearToSecond;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TimeBucket5 extends AbstractRoutine<OffsetDateTime> {
  private static final long serialVersionUID = 1L;

  /**
   * The parameter <code>public.time_bucket.RETURN_VALUE</code>.
   */
  public static final Parameter<OffsetDateTime> RETURN_VALUE =
      Internal.createParameter("RETURN_VALUE", SQLDataType.TIMESTAMPWITHTIMEZONE, false, false);

  /**
   * The parameter <code>public.time_bucket.bucket_width</code>.
   */
  public static final Parameter<YearToSecond> BUCKET_WIDTH =
      Internal.createParameter("bucket_width", SQLDataType.INTERVAL, false, false);

  /**
   * The parameter <code>public.time_bucket.ts</code>.
   */
  public static final Parameter<OffsetDateTime> TS =
      Internal.createParameter("ts", SQLDataType.TIMESTAMPWITHTIMEZONE, false, false);

  /**
   * The parameter <code>public.time_bucket.origin</code>.
   */
  public static final Parameter<OffsetDateTime> ORIGIN =
      Internal.createParameter("origin", SQLDataType.TIMESTAMPWITHTIMEZONE, false, false);

  /**
   * Create a new routine call instance
   */
  public TimeBucket5() {
    super("time_bucket", Public.PUBLIC, SQLDataType.TIMESTAMPWITHTIMEZONE);

    setReturnParameter(RETURN_VALUE);
    addInParameter(BUCKET_WIDTH);
    addInParameter(TS);
    addInParameter(ORIGIN);
    setOverloaded(true);
  }

  /**
   * Set the <code>bucket_width</code> parameter IN value to the routine
   */
  public void setBucketWidth(YearToSecond value) {
    setValue(BUCKET_WIDTH, value);
  }

  /**
   * Set the <code>bucket_width</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket5 setBucketWidth(Field<YearToSecond> field) {
    setField(BUCKET_WIDTH, field);
    return this;
  }

  /**
   * Set the <code>ts</code> parameter IN value to the routine
   */
  public void setTs(OffsetDateTime value) {
    setValue(TS, value);
  }

  /**
   * Set the <code>ts</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket5 setTs(Field<OffsetDateTime> field) {
    setField(TS, field);
    return this;
  }

  /**
   * Set the <code>origin</code> parameter IN value to the routine
   */
  public void setOrigin(OffsetDateTime value) {
    setValue(ORIGIN, value);
  }

  /**
   * Set the <code>origin</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket5 setOrigin(Field<OffsetDateTime> field) {
    setField(ORIGIN, field);
    return this;
  }
}
