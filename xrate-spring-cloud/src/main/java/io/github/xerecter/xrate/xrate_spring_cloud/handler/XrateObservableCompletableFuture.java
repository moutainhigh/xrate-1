package io.github.xerecter.xrate.xrate_spring_cloud.handler;

import com.netflix.hystrix.HystrixCommand;
import rx.Subscription;

import java.util.concurrent.CompletableFuture;

public class XrateObservableCompletableFuture<T> extends CompletableFuture<T> {

    private final Subscription sub;

    XrateObservableCompletableFuture(final HystrixCommand<T> command) {
        this.sub = command.toObservable().single().subscribe(XrateObservableCompletableFuture.this::complete,
                XrateObservableCompletableFuture.this::completeExceptionally);
    }


    @Override
    public boolean cancel(final boolean b) {
        sub.unsubscribe();
        return super.cancel(b);
    }

}
