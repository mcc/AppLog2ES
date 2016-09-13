/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hk.mcc.utils.applog2es;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder.FieldCaseConversion;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author cc
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String pathString = "G:\\tmp\\Archive20160902";
        Path path = Paths.get(pathString);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "app*.log")) {
            for (Path entry : stream) {
                List<AppLog> appLogs = new ArrayList<>();
                try (AppLogParser appLogParser = new AppLogParser(Files.newInputStream(entry))) {
                    AppLog nextLog = appLogParser.nextLog();
                    while (nextLog != null) {
                        //    System.out.println(nextLog);
                        nextLog = appLogParser.nextLog();
                        appLogs.add(nextLog);
                    }

                    post2ES(appLogs);
                } catch (IOException ex) {
                    Logger.getLogger(AppLogParser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    private static void post2ES(List<AppLog> appLogs) {
        try {
            // on startup
            DateTimeFormatter sdf = ISODateTimeFormat.dateTime();
            Settings settings = Settings.settingsBuilder()
                    .put("cluster.name", "my-application").build();
            //Add transport addresses and do something with the client...
            Client client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
            
            XContentBuilder mapping = jsonBuilder()
                              .startObject()
                                   .startObject("applog")
                                        .startObject("properties")
                                             .startObject("Url")
                                                .field("type","string")
                                                .field("index", "not_analyzed")
                                             .endObject()
                                             .startObject("Event")
                                                .field("type","string")
                                                .field("index", "not_analyzed")
                                             .endObject()
                                            .startObject("ClassName")
                                                .field("type","string")
                                                .field("index", "not_analyzed")
                                             .endObject()
                                            .startObject("UserId")
                                                .field("type","string")
                                                .field("index", "not_analyzed")
                                             .endObject()
                                            .startObject("Application")
                                                .field("type","string")
                                                .field("index", "not_analyzed")
                                             .endObject()
                                            .startObject("ecid")
                                                .field("type","string")
                                                .field("index", "not_analyzed")
                                             .endObject()
                                        .endObject()
                                    .endObject()
                                 .endObject();

            PutMappingResponse putMappingResponse = client.admin().indices()
                .preparePutMapping("applog")
                .setType("applog")
                .setSource(mapping)
                .execute().actionGet();
            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (AppLog appLog : appLogs) {
                // either use client#prepare, or use Requests# to directly build index/delete requests
                if (appLog != null) {
                    if (StringUtils.contains(appLog.getMessage(),"[CIMS_INFO] Filter Processing time")){
                        String[] split = StringUtils.split(appLog.getMessage(),",");
                        int elapsedTime = 0;
                        String url = "";
                        String event = "";
                        for (String token : split) {
                            if(StringUtils.contains(token, "elapsedTime")){
                                elapsedTime = Integer.parseInt(StringUtils.substringAfter(token, "="));
                            } else if(StringUtils.contains(token, "with URL")){
                                url = StringUtils.substringAfter(token, "=");
                            } else if(StringUtils.contains(token, "event")){
                                event = StringUtils.substringAfter(token, "=");
                            }
                        }
                        
                        bulkRequest.add(client.prepareIndex("applog", "applog")
                            .setSource(jsonBuilder()
                                    .startObject()
                                    .field("className", appLog.getClassName())
                                    .field("logTime", appLog.getLogTime())
                                    .field("application", appLog.getApplication())
                                    .field("code", appLog.getCode())
                                    .field("message", appLog.getMessage())
                                    .field("ecid", appLog.getEcid())
                                    .field("application", appLog.getApplication())
                                    .field("level", appLog.getLevel())
                                    .field("server", appLog.getServer())
                                    .field("tid", appLog.getTid())
                                    .field("userId", appLog.getUserId())
                                    .field("urls", url)
                                    .field("elapsedTime", elapsedTime)
                                    .field("events", event)
                                    .endObject())
                        );
                    }
                }
            }
            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, bulkResponse.buildFailureMessage());
                // process failures by iterating through each bulk response item
            }

// on shutdown
            client.close();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
