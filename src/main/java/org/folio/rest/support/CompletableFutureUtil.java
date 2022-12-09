package org.folio.rest.support;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

// dead code if not used as an artifact dependency
public final class CompletableFutureUtil {
  private CompletableFutureUtil() {}

  public static <T> Handler<AsyncResult<T>> mapFutureResultToJavaFuture(CompletableFuture<T> future) {
    return result -> {
      if (result.succeeded()) {
        future.complete(result.result());
      } else {
        future.completeExceptionally(result.cause());
      }
    };
  }
}
