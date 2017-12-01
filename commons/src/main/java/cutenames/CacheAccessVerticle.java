package cutenames;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import io.vertx.reactivex.core.AbstractVerticle;

public abstract class CacheAccessVerticle extends AbstractVerticle {

   protected RemoteCacheManager client;
   protected RemoteCache<String, String> defaultCache;

   @Override
   public void start() throws Exception {
     initCache();
   }

   protected void initCache() {
      vertx.executeBlocking(fut -> {
         Configuration configuration = new ConfigurationBuilder().addServer()
               .host(config().getString("infinispan.host", "datagrid-hotrod"))
               .port(config().getInteger("infinispan.port", 11222))
               .build();
         client = new RemoteCacheManager(
               configuration);

         defaultCache = client.getCache();
         defaultCache.put("42", "Oihana");

         addConfigToCache();
         fut.complete();
      }, res -> {
         if (res.succeeded()) {
            getLogger().log(Level.INFO, "Cache connection successfully done");
            initSuccess();
         } else {
            getLogger().log(Level.SEVERE, "Cache connection error", res.cause());
         }
      });
   }

   protected void addConfigToCache(){

   }

   protected abstract void initSuccess();

   protected abstract Logger getLogger();
}
