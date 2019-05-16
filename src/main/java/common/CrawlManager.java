package common;

import java.util.concurrent.Semaphore;

public class CrawlManager {
    private final Semaphore semaphore;

    public CrawlManager(int numThreads) {
        semaphore = new Semaphore(numThreads);
    }

    public Thread startProcess(final Runnable runCrawl) {
        ManagedTask task = new ManagedTask(runCrawl);
        Thread thread = new Thread(task);
        thread.start();

        return thread;
    }

    private class ManagedTask implements Runnable {

        private Runnable runCrawl;
        public ManagedTask(Runnable runCrawl) {
            this.runCrawl = runCrawl;
        }

        @Override
        public void run() {
            try {
                semaphore.acquire();
                runCrawl.run();
            } catch (Throwable t) {

            } finally {
                semaphore.release();
            }
        }
    }
}
