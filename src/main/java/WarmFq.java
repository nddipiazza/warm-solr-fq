import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;

import java.util.Arrays;
import java.util.List;

public class WarmFq {
  public static void main(String [] args) throws Exception {
    List<String> solrHosts = Arrays.asList("http://192.168.1.62:8983/solr", "http://192.168.1.62:8983", "http://192.168.1.64:8983/solr");
    String collection = "Enterprise_Search";

    String username = ClientUtils.escapeQueryChars("NA\\DALSNDSZ");

    Thread t1 = warmReplica(collection, username, solrHosts.get(0));
    Thread t2 = warmReplica(collection, username, solrHosts.get(1));
    Thread t3 = warmReplica(collection, username, solrHosts.get(2));

    t1.join();
    t2.join();
    t3.join();
  }

  private static Thread warmReplica(String collection, String username, String solrHost) {
    Thread t = new Thread(() -> {
      try (CloudSolrClient solrClient = new CloudSolrClient.Builder(Arrays.asList(solrHost))
          .withConnectionTimeout(10000)
          .withSocketTimeout(60000)
          .build()) {

        long started = System.currentTimeMillis();
        SolrQuery query = new SolrQuery();
        query.set("q", "*:*");
        query.set("fq", "{!join from=id to=_lw_acl_ss fromIndex=acl}{!graph from=inbound_ss to=outbound_ss}id:" + username);
        query.setRows(0);
        solrClient.query(collection, query);

        System.out.println("Finished in " + (System.currentTimeMillis() - started) +  " ms.");
      } catch (Exception e) {
        // Log it
      }
    });
    t.start();
    return t;
  }
}
