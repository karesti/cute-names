package cutenames;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class CuteNamesRestAPITest {

   private Vertx vertx;
   private int port;
   private HttpClient httpClient;
   private String host;

   @Before
   public void setUp(TestContext context) throws IOException {
      vertx = Vertx.vertx();
      ServerSocket socket = new ServerSocket(0);
      host = "127.0.0.1";
      port = socket.getLocalPort();
      socket.close();
      DeploymentOptions options = new DeploymentOptions()
            .setConfig(new JsonObject()
                  .put("http.port", port)
                  .put("infinispan.host", host)
            );
      httpClient = vertx.createHttpClient();
      vertx.deployVerticle(CuteNamesRestAPI.class.getName(), options, context.asyncAssertSuccess());
   }

   @After
   public void after(TestContext context) {
      vertx.close(context.asyncAssertSuccess());
   }

   @Test
   public void welcome_endpoint(TestContext context) {
      final Async async = context.async();
      httpClient.getNow(port, host, "/", response -> {
         response.handler(body -> {
            context.assertEquals(200, response.statusCode());
            context.assertEquals("text/html", response.headers().get("content-type"));
            context.assertTrue(body.toString().contains("Welcome"));
            async.complete();
         });
      });
   }

   @Test
   public void non_existing_endpoint(TestContext context) {
      final Async async = context.async();

      httpClient.getNow(port, host, "/nothing", response -> {
         response.handler(body -> {
            context.assertEquals(404, response.statusCode());
            async.complete();
         });
      });
   }

   @Test
   public void api_endpoint(TestContext context) {
      final Async async = context.async();
      httpClient.getNow(port, host, "/api", response -> {
         response.handler(body -> {
            context.assertEquals(200, response.statusCode());
            context.assertEquals("application/json", response.headers().get("content-type"));
            context.assertEquals("{\"name\":\"cutenames\",\"version\":1}", body.toString());
            async.complete();
         });
      });
   }

   @Test
   public void put_cute_name_endpoint(TestContext context) {
      final Async async = context.async();
      WebClient client = WebClient.wrap(httpClient);
      JsonObject body = new JsonObject().put("id", "123").put("name", "Fidelia");
      client.post(port, host, "/api/cutenames").sendJsonObject(body, ar -> {
         if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            context.assertEquals(201, response.statusCode());
            context.assertEquals("Cute name added", response.body().toString());
         } else {
            context.fail(ar.cause());
         }
         async.complete();
      });
   }

   @Test
   public void put_cute_name_with_bad_format(TestContext context) {
      final Async async = context.async();
      WebClient client = WebClient.wrap(httpClient);
      JsonObject body = new JsonObject().put("id", 123).put("name", "Bad");
      client.post(port, host, "/api/cutenames").sendJsonObject(body, ar -> {
         if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            context.assertEquals(500, response.statusCode());
         } else {
            context.fail(ar.cause());
         }
         async.complete();
      });
   }

   @Test
   public void put_cute_name_without_id_endpoint(TestContext context) {
      final Async async = context.async();
      WebClient client = WebClient.wrap(httpClient);
      JsonObject body = new JsonObject().put("name", "Elaia");
      client.post(port, host, "/api/cutenames").sendJsonObject(body, ar -> {
         if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            context.assertEquals(201, response.statusCode());
            context.assertEquals("Cute name added", response.body().toString());
         } else {
            context.fail(ar.cause());
         }
         async.complete();
      });
   }

   @Test
   public void put_cute_name_without_name(TestContext context) {
      final Async async = context.async();
      WebClient client = WebClient.wrap(httpClient);
      JsonObject emptyBody = new JsonObject();
      client.post(port, host, "/api/cutenames").sendJsonObject(emptyBody, ar -> {
         if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            context.assertEquals(400, response.statusCode());
            context.assertEquals("Body is {}. 'id' and 'name' should be provided", response.body().toString());
         } else {
            context.fail(ar.cause());
         }
         async.complete();
      });
   }

   @Test
   public void get_cute_name_with_id(TestContext context) {
      final Async async = context.async();
      httpClient.getNow(port, host, "/api/cutenames/42", response -> {
         response.handler(body -> {
            context.assertEquals(200, response.statusCode());
            context.assertEquals("{\"name\":\"Oihana\"}", body.toString());
            async.complete();
         });
      });
   }

   @Test
   public void get_cute_name_with_unexisting_id(TestContext context) {
      final Async async = context.async();
      httpClient.getNow(port, host, "/api/cutenames/666", response -> {
         response.handler(body -> {
            context.assertEquals(404, response.statusCode());
            context.assertEquals("Cute name 666 not found", body.toString());
            async.complete();
         });
      });
   }
}
