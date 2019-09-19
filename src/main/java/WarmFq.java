import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.asynchttpclient.AsyncHttpClient;

import java.util.List;
import java.util.Map;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class WarmFq {
  public static void main(String[] args) throws Exception {

    String username = ClientUtils.escapeQueryChars("USERNAME_HERE");

    String collectionName = "Enterprise_Search";
    String fusionVersion = "4.1.0";
    String zkConnect = "192.168.1.62:9983,192.168.1.63:9983,192.168.1.64:9983/lwfusion/" + fusionVersion;

    List<String> replicaUrls = Lists.newArrayList();

    ObjectMapper objectMapper = new ObjectMapper();

    try (CuratorFramework client = CuratorFrameworkFactory
        .builder()
        .connectString(zkConnect)
        .sessionTimeoutMs(15000)
        .connectionTimeoutMs(15000)
        .retryPolicy(new ExponentialBackoffRetry(1000, 3))
        .build()) {
      client.start();

      byte[] data = client.getData().forPath("/solr/collections/" + collectionName + "/state.json");

      Map stateMap = objectMapper.readValue(data, Map.class);

      Map colMap = (Map)stateMap.get(collectionName);

      Map shardsMap = (Map)colMap.get("shards");

      for (Object shard : shardsMap.values()) {

        Map replicasMap = (Map)((Map)shard).get("replicas");
        for (Object replicaObj : replicasMap.values()) {
          Map replica = (Map)replicaObj;

          String baseUrl = (String)replica.get("base_url");
          String core = (String)replica.get("core");

          replicaUrls.add(baseUrl + "/" + core);
        }

      }
    }

    AsyncHttpClient asyncHttpClient = asyncHttpClient();

    long time = System.currentTimeMillis();

    replicaUrls.parallelStream().forEach(replicaShardUrl -> {
      System.out.println(replicaShardUrl + "/select?q=*:*&rows=0&fq={!join%20from=id%20to=_lw_acl_ss%20fromIndex=acl}{!graph%20from=inbound_ss%20to=outbound_ss}id:" + username);
      asyncHttpClient.prepareGet(replicaShardUrl + "/select?q=*:*&rows=0&fq={!join%20from=id%20to=_lw_acl_ss%20fromIndex=acl}{!graph%20from=inbound_ss%20to=outbound_ss}id:" + username)
          .execute();
    });

    System.out.println("Done - took " + (System.currentTimeMillis() - time));


  }
}
