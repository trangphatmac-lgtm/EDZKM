/** Subprocess fixture used by the worker timeout test. */
class WorkerSleeper {
  public static void main(String[] args) throws InterruptedException {
    Thread.sleep(60000);
  }
}
