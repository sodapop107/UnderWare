package meteorclient.utils.network;

import meteorclient.utils.Init;
import meteorclient.utils.InitStage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnderWareExecutor {
    public static ExecutorService executor;

    @Init(stage = InitStage.Pre)
    public static void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    public static void execute(Runnable task) {
        executor.execute(task);
    }
}
