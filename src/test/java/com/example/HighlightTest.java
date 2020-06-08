package com.example;

import static com.example.HighlightSolrJettyTestBase.createSolrHome;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.apache.solr.SolrTestCaseJ4.DEFAULT_CONNECTION_TIMEOUT;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import org.junit.BeforeClass;
import org.junit.Test;

public class HighlightTest extends HighlightSolrJettyTestBase {

    private static final String CONFIG_SET_PATH = "src/test/configsets/highlight_configs";
    private static final String CORE_NAME = "highlight";

    @BeforeClass
    public static void beforeClass() throws Exception {
        createAndStartJetty(createSolrHome(CONFIG_SET_PATH, CORE_NAME, "src/test/resources"));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        index();
    }

    @Override
    public SolrClient createNewSolrClient() {
        return getHttpSolrClient(getCoreBaseUrl(CORE_NAME), DEFAULT_CONNECTION_TIMEOUT);
    }

    public void index() throws Exception {
        for (int i = 0; i < 10; i++) {
            SolrInputDocument doc = new SolrInputDocument();
            doc = new SolrInputDocument();
            doc.setField("id", UUID.randomUUID().toString());
            doc.setField("content_txt", Files.readString(Path.of("src/test/resources/doc2.txt")));
            assertUpd(addDoc(doc));

            doc = new SolrInputDocument();
            doc.setField("id", UUID.randomUUID().toString());
            doc.setField("content_txt", Files.readString(Path.of("src/test/resources/doc3.txt")));
            assertUpd(addDoc(doc));
        }

        assertUpd(commit());

        System.out.println(getCoreBaseUrl(CORE_NAME));
    }

    @Test
    public void unifiedHighlighter() throws Exception {
        // On my machine search with WORD highlighter takes less than 10 ms
        // search with SENTENCE highliter is about 30 ms so it's 3 - 5 times slower
        // search with optimized SENTENCE highliter is similar than WORD
        testDoc2();
        // Lets do the same with doc3
        // search with WORD highliter takes similar to doc3, about 10 ms
        // search with SENTENCE highliter takes about 220 ms which is more than 10 times slower than WORD
        // search with optimized SENTENCE highliter is similar than WORD
        testDoc3();
    }

    private void testDoc2() throws Exception {
        SolrQuery sentenceHighlightQuery = query("qt", "/search",
                "q", "english american", "hl.bs.type", "SENTENCE");

        SolrQuery optimizedSentenceHighlightQuery = query("qt", "/search",
                "q", "english american", "hl.bs.type", "SENTENCE", "hl.fragsizeIsMinimum", "true",
                "hl.fragAlignRatio", "0");

        SolrQuery wordHighlightQuery = query("qt", "/search",
                "q", "english american", "hl.bs.type", "WORD");

        int sentenceAvgTime = getAverateTime(sentenceHighlightQuery, is(10L), 3);
        int optSentenceAvgTime = getAverateTime(optimizedSentenceHighlightQuery, is(10L), 3);
        int wordAvgTime = getAverateTime(wordHighlightQuery, is(10L), 3);

        System.out.println("DOC2 sentenceAvgTime: " + sentenceAvgTime);
        System.out.println("DOC2 optSentenceAvgTime: " + optSentenceAvgTime);
        System.out.println("DOC2 wordAvgTime: " + wordAvgTime);

        // SENTENCE is less than 6 times slower
        assertThat(sentenceAvgTime, lessThan(6 * wordAvgTime));
        // optimized SENTENCE is similar to WORD
        assertThat(optSentenceAvgTime, lessThan(2 * wordAvgTime));
    }

    private void testDoc3() throws Exception {
        SolrQuery sentenceHighlightQuery = query("qt", "/search",
                "q", "slovenskej republiky", "hl.bs.type", "SENTENCE");

        SolrQuery optimizedSentenceHighlightQuery = query("qt", "/search",
                "q", "slovenskej republiky", "hl.bs.type", "SENTENCE", "hl.fragsizeIsMinimum", "true",
                "hl.fragAlignRatio", "0");

        SolrQuery wordHighlightQuery = query("qt", "/search",
                "q", "slovenskej republiky", "hl.bs.type", "WORD");

        int sentenceAvgTime = getAverateTime(sentenceHighlightQuery, is(10L), 3);
        int optSentenceAvgTime = getAverateTime(optimizedSentenceHighlightQuery, is(10L), 3);
        int wordAvgTime = getAverateTime(wordHighlightQuery, is(10L), 3);

        System.out.println(sentenceAvgTime);
        System.out.println(optSentenceAvgTime);
        System.out.println(wordAvgTime);

        // SENTENCE is more than 10 times slower
        assertThat(sentenceAvgTime, greaterThan(10 * wordAvgTime));
        // optimized SENTENCE is similar to WORD
        assertThat(optSentenceAvgTime, lessThan(2 * wordAvgTime));
    }

    private int getAverateTime(SolrQuery query, Matcher<Long> matcher, int count) throws Exception {
        int time = 0;
        for (int i = 0; i < count; i++) {
            QueryResponse resp = assertRespCount(query, matcher);
            time += (int) resp.getResponseHeader().get("QTime");
        }
        return time / count;
    }
}
