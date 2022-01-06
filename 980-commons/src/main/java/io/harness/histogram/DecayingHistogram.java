/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.histogram;

import static com.google.api.client.util.Preconditions.checkArgument;

import java.time.Duration;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A histogram that gives newer samples a higher weight than the old samples,
 * gradually decaying ("forgetting") the past samples. The weight of each sample
 * is multiplied by the factor of 2^((sampleTime - referenceTimestamp) / halfLife).
 * This means that the sample loses half of its weight ("importance") with
 * each halfLife period.
 * Since only relative (and not absolute) weights of samples matter, the
 * referenceTimestamp can be shifted at any time, which is equivalent to multiplying all
 * weights by a constant. In practice the referenceTimestamp is shifted forward whenever
 * the exponents become too large, to avoid floating point arithmetic overflow.
 */
@ToString
@EqualsAndHashCode
public class DecayingHistogram implements Histogram {
  // When decay factor exceeds 2^MAX_DECAY_EXPONENT, the histogram is
  // renormalized by shifting the reference time forward.
  private static final int MAX_DECAY_EXPONENT = 100;

  private final HistogramImpl histogram;

  // Decay half life period
  private final long halfLifeMs;

  // Reference time for determining the relative age of samples.
  // It is always an integer multiple of halfLife.
  private long referenceTimestampMs;

  public DecayingHistogram(HistogramOptions options, Duration halfLife) {
    this.histogram = new HistogramImpl(options);
    this.halfLifeMs = halfLife.toMillis();
    this.referenceTimestampMs = 0;
  }

  @Override
  public double getPercentile(double percentile) {
    return this.histogram.getPercentile(percentile);
  }

  @Override
  public void addSample(double value, double weight, Instant time) {
    this.histogram.addSample(value, weight * decayFactor(time), time);
  }

  @Override
  public void subtractSample(double value, double weight, Instant time) {
    this.histogram.subtractSample(value, weight * decayFactor(time), time);
  }

  @Override
  public void merge(Histogram other) {
    DecayingHistogram o = (DecayingHistogram) other;
    checkArgument(o.halfLifeMs == this.halfLifeMs, "Can't merge decaying histograms with different half-life periods");
    // Align the older referenceTimestamp with the younger one.
    if (this.referenceTimestampMs < o.referenceTimestampMs) {
      this.shiftReferenceTimestamp(o.referenceTimestampMs);
    } else if (o.referenceTimestampMs < this.referenceTimestampMs) {
      o.shiftReferenceTimestamp(this.referenceTimestampMs);
    }
    this.histogram.merge(o.histogram);
  }

  @Override
  public boolean isEmpty() {
    return this.histogram.isEmpty();
  }

  @Override
  public HistogramCheckpoint saveToCheckpoint() {
    HistogramCheckpoint checkpoint = this.histogram.saveToCheckpoint();
    return checkpoint.toBuilder().referenceTimestamp(Instant.ofEpochMilli(this.referenceTimestampMs)).build();
  }

  @Override
  public void loadFromCheckPoint(HistogramCheckpoint checkpoint) {
    this.histogram.loadFromCheckPoint(checkpoint);
    this.referenceTimestampMs = checkpoint.getReferenceTimestamp().toEpochMilli();
  }

  private void shiftReferenceTimestamp(long newReferenceTimestamp) {
    // Make sure decay start is an integer multiple of halfLife
    newReferenceTimestamp = roundToNearestMultiple(newReferenceTimestamp, this.halfLifeMs);
    double exp = Math.round((double) (this.referenceTimestampMs - newReferenceTimestamp) / (double) this.halfLifeMs);
    this.histogram.scale(Math.pow(2.0, exp)); // Scale all weights by 2^exp
    this.referenceTimestampMs = newReferenceTimestamp;
  }

  private long roundToNearestMultiple(long n, long d) {
    long r = n % d;
    if (r + r < d) {
      n -= r;
    } else {
      n += d - r;
    }
    return n;
  }

  private double decayFactor(Instant timestamp) {
    long timestampMs = timestamp.toEpochMilli();
    // Max timestamp before the exponent grows too large.
    long maxAllowedTimestampMs = this.referenceTimestampMs + (this.halfLifeMs * MAX_DECAY_EXPONENT);
    if (timestampMs > maxAllowedTimestampMs) {
      // The exponent has grown too large. Renormalize the histogram by
      // shifting the referenceTimestamp to the current timestamp and rescaling
      // the weights accordingly.
      shiftReferenceTimestamp(timestampMs);
    }
    long elapsed = timestampMs - this.referenceTimestampMs;
    return Math.pow(2, (double) elapsed / (double) this.halfLifeMs);
  }
}
