package io.piveau.translation.request;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationRequestVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(TranslationRequestVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    log.info("Start deployment of verticle " + TranslationRequestVerticle.class.getSimpleName());

    TranslationRequestService.create(vertx, ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder
          .setAddress(TranslationRequestService.SERVICE_ADDRESS)
          .register(TranslationRequestService.class, ready.result());
        log.info("Deployment of verticle " + TranslationRequestVerticle.class.getSimpleName() + " successful.");
        startFuture.complete();
      } else {
        log.error("Could not deploy verticle " + TranslationRequestVerticle.class.getSimpleName());
        startFuture.fail(ready.cause());
      }
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }
}
