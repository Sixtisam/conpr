package as.concrawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimalistic webserver providing web-crawling functionality to clients.<br/>
 * Point a browser to:<br/>
 * http://localhost:8080/
 */
public class ConCrawler {
    /** Port of the webserver. */
    private static final int PORT = 8080;

    static final String HEADER_OK = "HTTP/1.0 200 OK\r\nConnection: close\r\nServer: WebCrawler v0\r\nContent-Type: text/html\r\n\r\n";
    static final String HEADER_404 = "HTTP/1.0 404 Not Found\r\nConnection: close\r\nServer: WebCrawler v0\r\n";

    /** Replace this string in HTML_TEMPLATE to fill result section. */
    private static String RESULT_PLACEHOLDER = "<div id='RESULT_SECTION'/>\n";

    /** Website of the crawler. */
    private static String HTML_TEMPLATE = "<html>\n" +
            "  <body>\n" +
            "    <h1>ConCrawler</h1>\n" +
            "      <form action='' method='get'>\n" +
            "        <input type='text' name='q' size='60'/>\n" +
            "        <input type='submit' value='Crawl'/>\n" +
            "      </form>\n" +
            RESULT_PLACEHOLDER +
            "  </body>\n" +
            "</html>\n";

    /** The webcrawler instance. */
    private static Crawler crawler = new ParallelCrawler();

    private static final Pattern QUERY_PATTERN = Pattern.compile("GET /\\?q=(.*) HTTP/1\\.(0|1)");

    /** Starts the webserver. */
    public static void main(String[] args) throws IOException {
        final ConCrawler webServer = new ConCrawler();
        webServer.start();
    }

    /** Starts the request handler loop. */
    public void start() throws IOException {
        System.out.println("ConCrawler started: http://localhost:" + PORT);
        AtomicInteger ids = new AtomicInteger(1);
//        Executor svc = Executors.newCachedThreadPool();
        Executor svc = Executors.newFixedThreadPool(10, r -> new Thread(r, "Wordddker-" + ids.incrementAndGet()));
//        Executor svc = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 20, 1, TimeUnit.MINUTES,
//                new ArrayBlockingQueue<Runnable>(500));
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final Socket connection = serverSocket.accept();
                svc.execute(() -> {
                    try {
                        handleRequest(connection);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    /**
     * Handles a single request. *
     * 
     * @throws IOException
     */
    public void handleRequest(Socket connection) throws IOException {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            final String request = in.readLine();
            System.out.println("REQUEST: " + request);

            String response;

            if (isRoot(request)) {
                response = HEADER_OK + HTML_TEMPLATE;
            } else if (isQuery(request)) {
                final String query = extractQuery(request);
                final long startTime = System.currentTimeMillis();
                final List<String> urls = crawler.crawl(query);
                final long duration = System.currentTimeMillis() - startTime;
                response = formatResult(query, urls, duration);
            } else {
                response = HEADER_404;
            }

            System.out.println("RESPONSE: " + response);
            out.write(response);
            out.flush();
        }
    }

    /** Returns true if the given request is a query. */
    private boolean isQuery(String request) {
        return QUERY_PATTERN.matcher(request).matches();
    }

    /** Returns true if the given request matches root. */
    private boolean isRoot(String request) {
        return request.matches("GET / HTTP/1\\.(0|1)");
    }

    /** Returns an OK HTTP header accompanied by the result html content. */
    private String formatResult(String query, List<String> urls, long duration) {
        final String resultTitle = "<h2>URLs reachable from: " + query + "</h2>\n";

        String resultList = "<ol>";
        for (final String url : urls) {
            resultList += "<li><a href='" + url + "'/>" + url + "</a></li>\n";
        }
        resultList += "</ol>\n";

        final String processingTime = "Processing time: " + duration + "ms";

        final String body = HTML_TEMPLATE.replace(RESULT_PLACEHOLDER, resultTitle + resultList + processingTime);
        return HEADER_OK + body;
    }

    /** Extracts the query of the request. */
    private String extractQuery(String request) throws UnsupportedEncodingException {
        final Matcher matcher = QUERY_PATTERN.matcher(request);
        if (matcher.matches()) {
            String url = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
            if (!url.startsWith("http")) {
                url = "http://" + url;
            }
            return url;
        } else {
            return null;
        }
    }
}
