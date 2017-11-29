package cutenames;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;

public class SendCuteNamesAPI extends CacheAccessVerticle {

   public static final String CUTE_NAMES_ADDRESS = "cute-names";
   private final Logger logger = Logger.getLogger(SendCuteNamesAPI.class.getName());
   private final Queue<Integer> namesIds = new LinkedList<>();

   @Override
   protected void init() {
      logger.info("Starting SendCuteNamesAPI");
      Router router = Router.router(vertx);

      SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
      BridgeOptions options = new BridgeOptions();
      options.addOutboundPermitted(new PermittedOptions().setAddress(CUTE_NAMES_ADDRESS));
      sockJSHandler.bridge(options);
      router.route("/eventbus/*").handler(sockJSHandler);

      router.get("/listen").handler(this::listen);
   }

   private void listen(RoutingContext ctx) {
      vertx
            .rxExecuteBlocking(fut -> fut.complete(addCuteNamesListener()))
            .doOnSuccess(v -> vertx.setPeriodic(1000, l -> publishNames()))
            .subscribe(res ->
                        ctx.response().end("Listener started")
                  , t -> {
                     logger.log(Level.SEVERE, "Failed to start listener", t);
                     ctx.response().end("Failed to start listener");
                  });
   }

   private void publishNames() {
      vertx.<List<JsonObject>>executeBlocking(fut -> fut.complete(retrieveNewNames()), ar -> {
         if (ar.succeeded()) {
            vertx.eventBus().publish(CUTE_NAMES_ADDRESS, ar.result());
         }
      });
   }

   private List<JsonObject> retrieveNewNames() {
      Set<Integer> ids = new HashSet<>();
      for (int i = 0; i < 10 || namesIds.isEmpty(); i++) {
         ids.add(namesIds.poll());
      }
      return defaultCache.getAll(ids).entrySet().stream()
            .map(pos -> new JsonObject().put(pos.getKey().toString(), pos.getValue()))
            .collect(Collectors.toList());
   }

   @Override
   protected Logger getLogger() {
      return logger;
   }

   @ClientListener
   public final class CuteNamesListener {
      @ClientCacheEntryCreated
      @SuppressWarnings("unused")
      public void created(ClientCacheEntryCreatedEvent<Integer> e) {
         logger.log(Level.INFO, "cute name created " + e.getKey());
         namesIds.offer(e.getKey());
      }
   }

   private Void addCuteNamesListener() {
      defaultCache.addClientListener(new CuteNamesListener());
      logger.info("Added delayed train listener");
      return null;
   }

}
