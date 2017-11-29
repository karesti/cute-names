package cutenames;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class DuchessAPI extends AbstractVerticle {

   private static final Logger logger = Logger.getLogger(DuchessAPI.class.getName());

   protected RemoteCacheManager client;
   protected RemoteCache<Integer, String> defaultCache;

   @Override
   public void start() throws Exception {
      initCache(vertx);
      Router router = Router.router(vertx);

      router.get("/").handler(rc -> {
         rc.response().putHeader("content-type", "text/html")
               .end("Welcome");
      });

      router.get("/api").handler(rc -> {
         rc.response().putHeader("content-type", "application/json")
               .end(new JsonObject().put("name", "duchess").put("version", 1).encode());
      });

      router.route().handler(BodyHandler.create());
      router.post("/api/duchess").handler(this::handleAddDuchess);
      router.get("/api/duchess/:id").handler(this::handleGetDuchess);

      vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(config().getInteger("http.port", 8080));
   }

   private void handleGetDuchess(RoutingContext rc) {
      String id = rc.request().getParam("id");
      defaultCache.getAsync(Integer.parseInt(rc.request().getParam("id")))
            .thenAccept(value -> {
               String duchess;
               if (value == null) {
                  duchess = String.format("Duchess %s not found", id);
                  rc.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code());
               }else {
                  duchess = new JsonObject().put("Duchess", value).encode();
               }
               rc.response().end(duchess);
            });
   }

   private void handleAddDuchess(RoutingContext rc) {
      HttpServerResponse response = rc.response();
      JsonObject bodyAsJson = rc.getBodyAsJson();
      if (bodyAsJson != null && bodyAsJson.containsKey("id") && bodyAsJson.containsKey("name")) {
         defaultCache.putAsync(bodyAsJson.getInteger("id"), bodyAsJson.getString("name"))
               .thenAccept(s -> {
                  response.setStatusCode(HttpResponseStatus.CREATED.code()).end("Duchess Added");
               });
      } else {
         response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
               .end(String.format("Body is %s. 'id' and 'name' should be provided", bodyAsJson));
      }
   }

   @Override
   public void stop(Future<Void> stopFuture) throws Exception {
      if (client != null) {
         client.stopAsync().whenComplete((e, ex) -> stopFuture.complete());
      } else
         stopFuture.complete();
   }

   protected void initCache(Vertx vertx) {
      vertx.executeBlocking(fut -> {
         Configuration configuration = new ConfigurationBuilder().addServer()
               .host(config().getString("infinispan.host", "datagrid"))
               .port(config().getInteger("infinispan.port", 11222))
               .build();
         client = new RemoteCacheManager(
               configuration);

         defaultCache = client.getCache();
         defaultCache.put(42, "Oihana");
         fut.complete();
      }, res -> {
         if (res.succeeded()) {
            logger.log(Level.INFO, "Cache connection successfully done");
         } else {
            logger.log(Level.SEVERE, "Cache connection error", res.cause());
         }
      });
   }
}
