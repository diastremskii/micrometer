/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.dropwizard;

import com.codahale.metrics.Timer;
import io.micrometer.core.instrument.AbstractTimer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.histogram.DistributionStatisticConfig;
import io.micrometer.core.instrument.histogram.pause.PauseDetector;
import io.micrometer.core.instrument.util.TimeDecayingMax;
import io.micrometer.core.instrument.util.TimeUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DropwizardTimer extends AbstractTimer {
    private final Timer impl;
    private final AtomicLong totalTime = new AtomicLong(0);
    private final TimeDecayingMax max;

    DropwizardTimer(Id id, Timer impl, Clock clock, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector) {
        super(id, clock, distributionStatisticConfig, pauseDetector, TimeUnit.MILLISECONDS);
        this.impl = impl;
        this.max = new TimeDecayingMax(clock, distributionStatisticConfig);
    }

    @Override
    protected void recordNonNegative(long amount, TimeUnit unit) {
        if (amount >= 0) {
            impl.update(amount, unit);

            long nanoAmount = TimeUnit.NANOSECONDS.convert(amount, unit);
            max.record(nanoAmount, TimeUnit.NANOSECONDS);
            totalTime.addAndGet(nanoAmount);
        }
    }

    @Override
    public long count() {
        return impl.getCount();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        return TimeUtils.nanosToUnit(totalTime.get(), unit);
    }

    @Override
    public double max(TimeUnit unit) {
        return max.poll(unit);
    }
}
