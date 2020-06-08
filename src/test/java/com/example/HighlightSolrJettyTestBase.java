package com.example;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matcher;
import static org.junit.Assert.assertThat;

public class HighlightSolrJettyTestBase extends SolrJettyTestBase {

    protected final static String createSolrHome(String configSetPath, String coreName, String securityJsonPath) {
        String sourceHome = new File("").getAbsolutePath();
        String solrHome = null;
        try {
            File tempSolrHome = LuceneTestCase.createTempDir().toFile();
//            tempSolrHome.deleteOnExit();
            copyFileToDirectory(new File(sourceHome, "src/test/resources/solr.xml"), tempSolrHome);
            copyFileToDirectory(new File(sourceHome, securityJsonPath + "/security.json"), tempSolrHome);
            File collection1Dir = new File(tempSolrHome, coreName);
            FileUtils.forceMkdir(collection1Dir);

            File configSetDir = new File(sourceHome, configSetPath + "/conf");
            FileUtils.copyDirectoryToDirectory(configSetDir, collection1Dir);
            Properties props = new Properties();
            props.setProperty("name", coreName);
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(FileUtils.openOutputStream(new File(collection1Dir, "core.properties")), "UTF-8");
                props.store(writer, null);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception ignore) {
                    }
                }
            }
            solrHome = tempSolrHome.getAbsolutePath();
        } catch (Exception exc) {
            if (exc instanceof RuntimeException) {
                throw (RuntimeException) exc;
            } else {
                throw new RuntimeException(exc);
            }
        }

        return solrHome;
    }

    protected UpdateResponse commit() throws SolrServerException, IOException {
        return getSolrClient().commit();
    }

    protected UpdateResponse addDoc(SolrInputDocument solrDoc) throws SolrServerException, IOException {
        return getSolrClient().add(solrDoc);
    }

    protected void assertUpd(UpdateResponse resp) {
        assertThat(resp.getStatus(), is(0));
    }

    protected QueryResponse assertRespCount(SolrQuery query, Matcher<Long> matcher) throws Exception {
        QueryResponse response = getSolrClient().query(query);
        assertThat(response.getResults().getNumFound(), matcher);
        return response;
    }

    protected SolrQuery query(String... fieldsAndValues) {
        assert fieldsAndValues.length % 2 == 0;
        SolrQuery query = new SolrQuery();
        for (int i = 0; i < fieldsAndValues.length; i += 2) {
            query.set(fieldsAndValues[i], fieldsAndValues[i + 1]);
        }
        return query;
    }

    protected static String getCoreBaseUrl(String coreName) {
        return jetty.getBaseUrl() + "/" + coreName;
    }
}
