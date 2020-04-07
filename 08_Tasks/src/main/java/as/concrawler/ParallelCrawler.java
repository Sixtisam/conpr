package as.concrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParallelCrawler implements Crawler {
    /** Maximal number of visited urls per request. */
    private static final int MAX_VISITS = 20;
//    public static final SeqCrawler SEQ_CRAWLER = new SeqCrawler();
    public final static AtomicInteger ID = new AtomicInteger(1);

    @Override
    public List<String> crawl(String startURL) {
        // do create a own pool because of helpQuiesce
        ForkJoinPool pool = ForkJoinPool.commonPool();
        final Set<String> crawledUrls = ConcurrentHashMap.newKeySet();
        pool.invoke(new PageCrawlerTask(startURL, crawledUrls));
        return new ArrayList<String>(crawledUrls);
    }

    /**
     * Parses the passed url and for each url on that page, a new PageCrawlerTask is
     * forked.
     *
     */
    public class PageCrawlerTask extends RecursiveAction {
        private static final long serialVersionUID = 1L;
        public final String url;
        public final Set<String> visited;

        public PageCrawlerTask(final String url, final Set<String> visited) {
            this.url = url;
            this.visited = visited;
            System.out.println("New instance " + ID.getAndIncrement());
        }

        @Override
        protected void compute() {
            try {
                final Document doc = Jsoup.parse(Jsoup.connect(url)
                        .header("Accept", "text/html")
                        .userAgent("ConCrawler/0.1 Mozilla/5.0")
                        .timeout(3000)
                        .get().html());

                final Elements links = doc.select("a[href]");
                List<PageCrawlerTask> tasks = new ArrayList<>();
                for (final Element link : links) {
                    final String linkString = link.absUrl("href");
                    if ((!visited.contains(linkString)) && linkString.startsWith("http")
                            && visited.size() < MAX_VISITS) {
                        visited.add(linkString);
                        tasks.add(new PageCrawlerTask(linkString, visited));
                    }
                }
                for (int j = 1; j < tasks.size(); j++) {
                    tasks.get(j).fork();
                }
                if (tasks.size() >= 1) {
                    tasks.get(0).invoke();
                }
                for (int j = 1; j < tasks.size(); j++) {
                    tasks.get(j).join();
                }

            } catch (final Exception e) {
                System.out.println("Problem reading '" + url + "'. Message: " + e.getMessage());
                e.printStackTrace();
            }
        }

    }

}
