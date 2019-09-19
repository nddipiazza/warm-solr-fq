import org.apache.solr.client.solrj.util.ClientUtils;
import org.asynchttpclient.AsyncHttpClient;

import java.util.Arrays;
import java.util.List;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class WarmFq {
  public static void main(String [] args) throws Exception {
    List<String> replicaShardUrls = Arrays.asList("http://192.168.1.62:8983/solr/Enterprise_Search_shard2_replica_n5",
        "http://192.168.1.63:8983/solr/Enterprise_Search_shard1_replica_n1",
        "http://192.168.1.63:8983/solr/Enterprise_Search_shard2_replica_n7",
        "http://192.168.1.64:8983/solr/Enterprise_Search_shard1_replica_n3");

    String username = ClientUtils.escapeQueryChars("YOUR USER NAMES HERE!");

    for (String replicaShardUrl : replicaShardUrls) {

      AsyncHttpClient asyncHttpClient = asyncHttpClient();

      System.out.println(asyncHttpClient.prepareGet(replicaShardUrl + "/select?q=*:*&rows=0&fq={!join%20from=id%20to=_lw_acl_ss%20fromIndex=acl}{!graph%20from=inbound_ss%20to=outbound_ss}id:" + username)
          .execute().get());
    }

    System.out.println("Done");


  }
}
