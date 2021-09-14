/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.routines;

import io.harness.timescaledb.Public;

import org.jooq.Field;
import org.jooq.Parameter;
import org.jooq.impl.AbstractRoutine;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TimeBucket11 extends AbstractRoutine<Integer> {
  private static final long serialVersionUID = 1L;

  /**
   * The parameter <code>public.time_bucket.RETURN_VALUE</code>.
   */
  public static final Parameter<Integer> RETURN_VALUE =
      Internal.createParameter("RETURN_VALUE", SQLDataType.INTEGER, false, false);

  /**
   * The parameter <code>public.time_bucket.bucket_width</code>.
   */
  public static final Parameter<Integer> BUCKET_WIDTH =
      Internal.createParameter("bucket_width", SQLDataType.INTEGER, false, false);

  /**
   * The parameter <code>public.time_bucket.ts</code>.
   */
  public static final Parameter<Integer> TS = Internal.createParameter("ts", SQLDataType.INTEGER, false, false);

  /**
   * The parameter <code>public.time_bucket.offset</code>.
   */
  public static final Parameter<Integer> OFFSET = Internal.createParameter("offset", SQLDataType.INTEGER, false, false);

  /**
   * Create a new routine call instance
   */
  public TimeBucket11() {
    super("time_bucket", Public.PUBLIC, SQLDataType.INTEGER);

    setReturnParameter(RETURN_VALUE);
    addInParameter(BUCKET_WIDTH);
    addInParameter(TS);
    addInParameter(OFFSET);
    setOverloaded(true);
  }

  /**
   * Set the <code>bucket_width</code> parameter IN value to the routine
   */
  public void setBucketWidth(Integer value) {
    setValue(BUCKET_WIDTH, value);
  }

  /**
   * Set the <code>bucket_width</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket11 setBucketWidth(Field<Integer> field) {
    setField(BUCKET_WIDTH, field);
    return this;
  }

  /**
   * Set the <code>ts</code> parameter IN value to the routine
   */
  public void setTs(Integer value) {
    setValue(TS, value);
  }

  /**
   * Set the <code>ts</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket11 setTs(Field<Integer> field) {
    setField(TS, field);
    return this;
  }

  /**
   * Set the <code>offset</code> parameter IN value to the routine
   */
  public void setOffset(Integer value) {
    setValue(OFFSET, value);
  }

  /**
   * Set the <code>offset</code> parameter to the function to be used with a {@link org.jooq.Select} statement
   */
  public TimeBucket11 setOffset(Field<Integer> field) {
    setField(OFFSET, field);
    return this;
  }
}
