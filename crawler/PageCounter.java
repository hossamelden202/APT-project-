package crawler;

import java.util.concurrent.atomic.AtomicInteger;

public class PageCounter {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final int limit;

    public PageCounter(int limit) {
        this.limit = limit;
    }

    public boolean incrementIfBelowLimit() {
        int current = counter.incrementAndGet();
        return current <= limit;
    }

    public boolean isLimitReached() {
        return counter.get() >= limit;
    }

    public int getCount() {
        return counter.get();
    }
}
