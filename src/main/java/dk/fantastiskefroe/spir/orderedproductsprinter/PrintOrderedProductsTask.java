package dk.fantastiskefroe.spir.orderedproductsprinter;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import dk.fantastiskefroe.spir.client.ingest.api.OrderControllerApi;
import dk.fantastiskefroe.spir.client.ingest.model.Order;
import dk.fantastiskefroe.spir.client.ingest.model.OrderLine;
import dk.fantastiskefroe.spir.orderedproductsprinter.config.ApplicationProperties;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PrintOrderedProductsTask {

    private final PebbleEngine pebbleEngine;
    private final ApplicationProperties applicationProperties;
    private final PdfRendererBuilder pdfBuilder;
    public PrintOrderedProductsTask(ApplicationProperties applicationProperties, PebbleEngine pebbleEngine) {
        this.pebbleEngine = pebbleEngine;
        this.applicationProperties = applicationProperties;

        // disable pdf renderer logging
        XRLog.setLoggingEnabled(false);
        this.pdfBuilder = new PdfRendererBuilder();
        this.pdfBuilder.useSVGDrawer(new BatikSVGDrawer());
        this.pdfBuilder.useFastMode();

    }

    private int i = 0;
    private static final Logger log = LogManager.getLogger(PrintOrderedProductsTask.class);

//    @Scheduled(cron = "0 0 12 * * MON-FRI")
    @Scheduled(cron = "${app.print-schedule}")
    public void reportCurrentTime() throws IOException {

        OrderControllerApi client = new OrderControllerApi();
        client.getApiClient().setBasePath(applicationProperties.orderApi().baseUri());

        client.getOrders("NULL")
                .collectList()
                .map(this::ordersToOrderlines)
                .map(this::orderlinesToHtmlTable)
                .doOnError(throwable -> log.error("API failed: "+throwable.getMessage()))
                .onErrorResume(throwable -> Mono.just("<h1>Failed to connect to database</h1><p class=\"exception\">"+throwable.getMessage()+"</p>"))
                .subscribe(this::printHtml);

    }


    private List<OrderLine> ordersToOrderlines(List<Order> orders) {
        log.info("got " + orders.size() + " orders");
        return orders.stream()
                .flatMap(order -> order.getOrderLines().stream())
                .collect(Collectors.groupingBy(OrderLine::getSku))
                .values().stream()
                .map(orderLines -> orderLines.stream()
                        .reduce((a,b) -> {a.setQuantity(a.getQuantity() + b.getQuantity()); return a;})
                ).map(maybeLine -> maybeLine.orElseThrow())
                .toList();
    }

    private String orderlinesToHtmlTable(List<OrderLine> orderLines) {
        PebbleTemplate template = pebbleEngine.getTemplate("orderlines");
        Map<String, Object> context = new HashMap<>();
        context.put("orderLines", orderLines.stream().map(line ->
                Map.ofEntries(
                        Map.entry("sku", line.getSku()),
                        Map.entry("title", line.getTitle()),
                        Map.entry("quantity", line.getQuantity())
                )).toList());

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }

    private void printHtml(String html) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try  {
            pdfBuilder.withHtmlContent(html, "/");
            pdfBuilder.toStream(os);
            pdfBuilder.run();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final SSHClient sshClient = new SSHClient();
        try {
            sshClient.setConnectTimeout(5);
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            final KeyProvider keyProvider = sshClient.loadKeys(applicationProperties.printServer().privateKey(), null, null);
            sshClient.connect(applicationProperties.printServer().host());
            sshClient.authPublickey(applicationProperties.printServer().username(), keyProvider);
            sshClient.newSCPFileTransfer().upload(new InMemoryFileFromString(os), applicationProperties.printServer().printerPath());
        }
        catch (IOException e) {
            log.error("Failed to print file. Saving locally instead");
            try {
                OutputStream fs = new FileOutputStream(new File("./orderedProducts.pdf").getAbsolutePath());
                fs.write(os.toByteArray());
                fs.flush();
                fs.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        finally {
            try {
                sshClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}