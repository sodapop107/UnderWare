package meteorclient.utils.network;

public class OnlinePlayers {
    private static long lastPingTime;

    public static void update() {
        long time = System.currentTimeMillis();

        if (time - lastPingTime > 5 * 60 * 1000) {
            UnderWareExecutor.execute(() -> Http.post("https://meteorclient.com/api/online/ping").send());

            lastPingTime = time;
        }
    }

    public static void leave() {
        UnderWareExecutor.execute(() -> Http.post("https://meteorclient.com/api/online/leave").send());
    }
}
