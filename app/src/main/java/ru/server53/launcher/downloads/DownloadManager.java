package ru.server53.launcher.downloads;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;


public class DownloadManager {

    private final Duration CONNECT_TIMEOUT;
    private final Duration REQUEST_TIMEOUT;
    
    private final HttpClient client;
    private final RateController rateController;


    DownloadManager(
        Duration connectTimeout,
        Duration requestTimeout
    ) {
        CONNECT_TIMEOUT = connectTimeout;
        REQUEST_TIMEOUT = requestTimeout;

        client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build()
        ;
        rateController = new RateController();
    }

    public static DownloadManager ofDefaultConfiguration() {
        return new DownloadManager(
            Duration.ofSeconds(10),
            Duration.ofSeconds(20)
        );
    }

    public CompletableFuture<HttpResponse<Path>> downloadFile(URI uri, Path filePath) {
        HttpRequest request = buildRequest(uri);

        try {
            rateController.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }

        return client.sendAsync(request, BodyHandlers.ofFile(filePath))
        .whenComplete((response, error) -> {
            try {
                if (error == null) {
                    long bytes = Files.size(filePath);
                    rateController.recordReceivedBytes(bytes);
                }
            } catch (IOException e) {
                // log if needed
            } finally {
                rateController.release();
            }
        });
    }

    private HttpRequest buildRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .build()
        ;
    }
}


class RateController implements AutoCloseable {
    private static final int MIN_ALLOWED_THREADS = 16;
    private static final int MAX_ALLOWED_THREADS = 96;
    private static final int THREADS_STEP_SIZE = 8;
    private static final long ROTATION_MS = 5_000;

    private final DynamicSemaphore semaphore;

    private final AtomicLong bytesFromLastRotation = new AtomicLong();
    private volatile float oldBPS = 0;
    private volatile float newBPS = 0;
    private int cooldownRotations = 0;

    private final Thread rotatorThread;
    private volatile boolean rotatorRunning = true;

    RateController() {
        semaphore = new DynamicSemaphore(MIN_ALLOWED_THREADS);
        rotatorThread = Thread.ofPlatform().daemon().start(() -> {
            while (rotatorRunning) {
                try {
                    Thread.sleep(ROTATION_MS);
                    System.out.println(
                        "[RateController] BFLR: %d, oldBPS: %f, newBPS: %f, limit: %d th"
                        .formatted(
                            bytesFromLastRotation.get(),
                            oldBPS, newBPS,
                            semaphore.getLimit()
                        )
                    );
                    rotate();
                } catch (InterruptedException e) {
                    if (!rotatorRunning) {
                        return;
                    }
                }
            }
        });
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    public void release() {
        semaphore.release();
    }

    private synchronized void rotate() throws InterruptedException {
        long bytes = bytesFromLastRotation.getAndSet(0);

        oldBPS = newBPS;
        newBPS = bytes * 1000f / ROTATION_MS;

        if (cooldownRotations > 0) {
            cooldownRotations--;
            return;
        }

        if (oldBPS > 0) {
            float change = (newBPS - oldBPS) / oldBPS;

            if (change <= -0.1f) {
                decreaseLimit();
                cooldownRotations = 0; // wait 20 seconds before probing again
                return;
            }
        }

        if (newBPS > 0) {
            increaseLimit();
        }
    }

    private void increaseLimit() throws InterruptedException {
        int currentLimit = semaphore.getLimit();
        if ((currentLimit+THREADS_STEP_SIZE) <= MAX_ALLOWED_THREADS) {
            semaphore.setLimit(currentLimit + THREADS_STEP_SIZE);
        }
    }

    private void decreaseLimit() throws InterruptedException {
        int currentLimit = semaphore.getLimit();
        if ((currentLimit-THREADS_STEP_SIZE) >= MIN_ALLOWED_THREADS) {
            semaphore.setLimit(currentLimit - THREADS_STEP_SIZE);
        }
    }

    public void recordReceivedBytes(long bytes) {
        bytesFromLastRotation.addAndGet(bytes);
    }

    @Override
    public void close() {
        rotatorRunning = false;
        rotatorThread.interrupt();
    }
}


final class DynamicSemaphore {

    private final Semaphore semaphore;
    private volatile int limit;

    public DynamicSemaphore(int initialLimit) {
        this.limit = initialLimit;
        this.semaphore = new Semaphore(initialLimit);
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }

    public void release() {
        semaphore.release();
    }

    public synchronized void setLimit(int newLimit) throws InterruptedException {
        int delta = newLimit - limit;

        if (delta > 0) {
            semaphore.release(delta);
        } else if (delta < 0) {
            semaphore.acquire(-delta);
        }

        limit = newLimit;
    }

    public int getLimit() {
        return limit;
    }

    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }
}