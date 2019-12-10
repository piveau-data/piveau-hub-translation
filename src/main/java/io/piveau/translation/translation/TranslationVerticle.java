package io.piveau.translation.translation;

import io.piveau.translation.util.ConfigConstant;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TranslationVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(TranslationVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    log.info("Start deployment of verticle " + TranslationVerticle.class.getSimpleName());

    TranslationService.create(vertx, config().getJsonObject(ConfigConstant.ETRANSLATION), ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder
          .setAddress(TranslationService.SERVICE_ADDRESS)
          .register(TranslationService.class, ready.result());
        log.info("Deployment of verticle " + TranslationVerticle.class.getSimpleName() + " successful.");
        startFuture.complete();
      } else {
        log.error("Could not deploy verticle " + TranslationVerticle.class.getSimpleName());
        startFuture.fail(ready.cause());
      }
    });
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }
}
