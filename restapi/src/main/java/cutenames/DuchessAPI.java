package cutenames;

import java.util.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

public class DuchessAPI extends CacheAccessVerticle {

   private final Logger logger = Logger.getLogger(DuchessAPI.class.getName());

   @Override
   protected void init() {
      logger.info("Starting DuchessAPI");
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
               } else {
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

   @Override
   protected Logger getLogger() {
      return logger;
   }
}
