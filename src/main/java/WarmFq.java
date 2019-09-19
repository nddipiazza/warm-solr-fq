import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class WarmFq {
  public static void main(String [] args) throws Exception {
    List<String> zkHosts = Arrays.asList("192.168.1.62:9983", "192.168.1.63:9983", "192.168.1.64:9983");
    String solrChRoot = "/lwfusion/4.1.0/solr";
    String collection = "Enterprise_Search";

    String username = ClientUtils.escapeQueryChars("usernamehere");

    try (CloudSolrClient solrClient = new CloudSolrClient.Builder(zkHosts, Optional.of(solrChRoot))
        .withConnectionTimeout(10000)
        .withSocketTimeout(60000)
        .build()) {

      long started = System.currentTimeMillis();
      SolrQuery query = new SolrQuery();
      query.set("q", "*:*");
      query.set("fq", "{!join from=id to=_lw_acl_ss fromIndex=acl}{!graph from=inbound_ss to=outbound_ss}id:" + username);
      query.setRows(0);
      query.set("shards", "192.168.1.62:8983/solr/Enterprise_Search,192.168.1.63:8983/solr/Enterprise_Search,192.168.1.64:8983/solr/Enterprise_Search");

      solrClient.query(collection, query);

      System.out.println("Finished in " + (System.currentTimeMillis() - started) " ms.");
    }
  }
}
