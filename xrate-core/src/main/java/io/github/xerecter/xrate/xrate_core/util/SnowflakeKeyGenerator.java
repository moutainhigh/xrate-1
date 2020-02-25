package io.github.xerecter.xrate.xrate_core.util;

import com.google.common.base.Preconditions;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 雪花算法生成器，直接搬的sharding jdbc的  (´・ω・`)
 */
public final class SnowflakeKeyGenerator {

    private SnowflakeKeyGenerator() {
    }

    private static SnowflakeKeyGenerator snowflakeKeyGenerator = null;

    private static ReentrantLock lock = new ReentrantLock(true);

    public static SnowflakeKeyGenerator getInstance() {
        if (snowflakeKeyGenerator != null) {
            return snowflakeKeyGenerator;
        }
        try {
            lock.lock();
            if (snowflakeKeyGenerator == null) {
                snowflakeKeyGenerator = new SnowflakeKeyGenerator();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return snowflakeKeyGenerator;
    }

    private static final long EPOCH;

    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_BITS = 10L;

    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;

    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;

    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;

    private static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;

    @Setter
    private static TimeService timeService = new TimeService();

    private static long workerId;

    private static int maxTolerateTimeDifferenceMilliseconds = 10;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.NOVEMBER, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH = calendar.getTimeInMillis();
    }

    private byte sequenceOffset;

    private long sequence;

    private long lastMilliseconds;

    /**
     * Set work process id.
     *
     * @param workerId work process id
     */
    public static void setWorkerId(final long workerId) {
        Preconditions.checkArgument(workerId >= 0L && workerId < WORKER_ID_MAX_VALUE);
        SnowflakeKeyGenerator.workerId = workerId;
    }

    /**
     * Set max tolerate time difference milliseconds.
     *
     * @param maxTolerateTimeDifferenceMilliseconds max tolerate time difference milliseconds
     */
    public static void setMaxTolerateTimeDifferenceMilliseconds(final int maxTolerateTimeDifferenceMilliseconds) {
        SnowflakeKeyGenerator.maxTolerateTimeDifferenceMilliseconds = maxTolerateTimeDifferenceMilliseconds;
    }

    /**
     * Generate key.
     *
     * @return key type is @{@link Long}.
     */
    public synchronized Number generateKey() {
        long currentMilliseconds = timeService.getCurrentMillis();
        if (waitTolerateTimeDifferenceIfNeed(currentMilliseconds)) {
            currentMilliseconds = timeService.getCurrentMillis();
        }
        if (lastMilliseconds == currentMilliseconds) {
            if (0L == (sequence = (sequence + 1) & SEQUENCE_MASK)) {
                currentMilliseconds = waitUntilNextTime(currentMilliseconds);
            }
        } else {
            vibrateSequenceOffset();
            sequence = sequenceOffset;
        }
        lastMilliseconds = currentMilliseconds;
        return ((currentMilliseconds - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
    }

    @SneakyThrows
    private boolean waitTolerateTimeDifferenceIfNeed(final long currentMilliseconds) {
        if (lastMilliseconds <= currentMilliseconds) {
            return false;
        }
        long timeDifferenceMilliseconds = lastMilliseconds - currentMilliseconds;
        Preconditions.checkState(timeDifferenceMilliseconds < maxTolerateTimeDifferenceMilliseconds,
                "Clock is moving backwards, last time is %d milliseconds, current time is %d milliseconds", lastMilliseconds, currentMilliseconds);
        Thread.sleep(timeDifferenceMilliseconds);
        return true;
    }

    private long waitUntilNextTime(final long lastTime) {
        long result = timeService.getCurrentMillis();
        while (result <= lastTime) {
            result = timeService.getCurrentMillis();
        }
        return result;
    }

    private void vibrateSequenceOffset() {
        sequenceOffset = (byte) (~sequenceOffset & 1);
    }

    private static class TimeService {

        /**
         * Get current millis.
         *
         * @return current millis
         */
        long getCurrentMillis() {
            return System.currentTimeMillis();
        }
    }

}
