package com.hallsync.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Centralized Thread Pool for HallSync multithreading.
 *
 * Why a Thread Pool?
 * - Instead of creating and destroying new Threads repeatedly (new Thread(...).start()),
 *   a thread pool reuses a fixed group of background worker threads.
 * - This prevents high CPU/memory overhead and keeps thread management in one place.
 * - Uses daemon threads so they automatically shut down when the app closes.
 *
 * Clean Syntax:
 *     AppExecutor.runAsync(
 *         () -> Database.query(),            // 1. Runs in background thread pool
 *         data -> { updateUI(data); },       // 2. Runs on JavaFX UI thread on success
 *         error -> { showError(error); }     // 3. Runs on JavaFX UI thread on error
 *     );
 */
public final class AppExecutor {

    /**
     * Shared fixed thread pool — up to 4 concurrent background worker threads.
     * Named threads for easy debugging (e.g., "hallsync-worker-1").
     */
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true); // Pool threads won't block JavaFX application shutdown
        thread.setName("hallsync-worker-" + thread.getId());
        return thread;
    });

    private AppExecutor() {
        // Utility class: do not instantiate
    }

    /**
     * Runs background work on the thread pool, then safely updates the JavaFX UI.
     *
     * @param backgroundWork A lambda returning the data fetched (runs on background thread)
     * @param onSuccess      A lambda accepting the result (guaranteed to run on JavaFX UI thread)
     * @param onError        A lambda accepting any exception (runs on JavaFX UI thread)
     */
    public static <T> void runAsync(Callable<T> backgroundWork, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return backgroundWork.call();
            }
        };

        if (onSuccess != null) {
            task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        }

        if (onError != null) {
            task.setOnFailed(e -> onError.accept(task.getException()));
        }

        POOL.submit(task);
    }

    /**
     * Convenience overload when custom error handling is not required.
     */
    public static <T> void runAsync(Callable<T> backgroundWork, Consumer<T> onSuccess) {
        runAsync(backgroundWork, onSuccess, Throwable::printStackTrace);
    }

    /**
     * Directly submit a JavaFX Task to the thread pool.
     */
    public static void run(Task<?> task) {
        POOL.submit(task);
    }

    /**
     * Gracefully shuts down the thread pool on application exit.
     */
    public static void shutdown() {
        POOL.shutdownNow();
    }
}
