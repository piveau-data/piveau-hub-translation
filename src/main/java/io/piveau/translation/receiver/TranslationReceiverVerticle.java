package io.piveau.translation.receiver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TranslationReceiverVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(TranslationReceiverVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    log.info("Start deployment of verticle " + TranslationReceiverVerticle.class.getSimpleName());

    TranslationReceiverService.create(vertx, ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder
          .setAddress(TranslationReceiverService.SERVICE_ADDRESS)
          .register(TranslationReceiverService.class, ready.result());
        log.info("Deployment of verticle " + TranslationReceiverVerticle.class.getSimpleName() + " successful.");
        startFuture.complete();
      } else {
        log.error("Could not deploy verticle " + TranslationReceiverVerticle.class.getSimpleName());
        startFuture.fail(ready.cause());
      }
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }
}
