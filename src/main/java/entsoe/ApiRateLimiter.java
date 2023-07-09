package entsoe;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ApiRateLimiter {

    private final AtomicReference<Instant> lastUsed = new AtomicReference<>(Instant.now());

    private final AtomicLong counter = new AtomicLong(0);

    private final int maxRequestsPerDuration;
    private final Duration duration;
    private final Duration minWaitTimeBetweenRequests;

    public ApiRateLimiter(int maxRequestsPerDuration, Duration duration, Duration minWaitTimeBetweenRequests) {
        this.maxRequestsPerDuration = maxRequestsPerDuration;
        this.duration = duration;
        this.minWaitTimeBetweenRequests = minWaitTimeBetweenRequests;
    }

    synchronized boolean acquire() {
        long newCounter = counter.incrementAndGet();
        try {
            Thread.sleep(minWaitTimeBetweenRequests.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (lastUsed.get().isBefore(Instant.now().minus(duration)) && newCounter>maxRequestsPerDuration) {
            counter.set(0);
            lastUsed.set(Instant.now());
            System.out.println("reset");
            return false;
        } else {
            return newCounter <= maxRequestsPerDuration;
        }
    }

    public synchronized void acquireWait() {
        while (!acquire()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
