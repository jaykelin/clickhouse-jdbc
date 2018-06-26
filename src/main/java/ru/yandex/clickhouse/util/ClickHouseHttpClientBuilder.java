package ru.yandex.clickhouse.util;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectionKeepAliveStrategy;

/*import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;*/

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;


public class ClickHouseHttpClientBuilder {

    private final ClickHouseProperties properties;

    public ClickHouseHttpClientBuilder(ClickHouseProperties properties) {
        this.properties = properties;
    }

//    public CloseableHttpClient buildClient() {
//        return HttpClientBuilder.create()
//                .setConnectionManager(getConnectionManager())
//                .setKeepAliveStrategy(createKeepAliveStrategy())
//                .setDefaultConnectionConfig(getConnectionConfig())
//                .setDefaultRequestConfig(getRequestConfig())
//                .disableContentCompression() // gzip здесь ни к чему. Используется lz4 при compress=1
//                .build();
//    }

    // 请求超时（s）
    private final int REQUEST_TIMEOUT = 10*1000;
    // 等待数据超时时间(S)
    private final int SO_TIMEOUT = 10*1000;

    public HttpClient buildClient(){
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "HttpComponents/1.1");
        HttpProtocolParams.setUseExpectContinue(params, true);

        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, properties.getConnectionTimeout());
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, properties.getSocketTimeout());
        params.setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, properties.getBufferSize());


        SchemeRegistry schreg = new SchemeRegistry();
        schreg.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schreg.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        PoolingClientConnectionManager pccm = new PoolingClientConnectionManager(schreg);
        pccm.setDefaultMaxPerRoute(properties.getDefaultMaxPerRoute());
//        pccm.setMaxPerRoute(properties.getDefaultMaxPerRoute());
        pccm.setMaxTotal(properties.getMaxTotal());

        DefaultHttpClient client =  new DefaultHttpClient(pccm, params);
        client.setKeepAliveStrategy(createKeepAliveStrategy());
        return client;

    }

//    private PoolingHttpClientConnectionManager getConnectionManager() {
//        //noinspection resource
//        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
//                RegistryBuilder.<ConnectionSocketFactory>create()
//                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
//                        .register("https", SSLConnectionSocketFactory.getSocketFactory())
//                        .build(),
//                null, null, new IpVersionPriorityResolver(), properties.getTimeToLiveMillis(), TimeUnit.MILLISECONDS);
//        connectionManager.setDefaultMaxPerRoute(properties.getDefaultMaxPerRoute());
//        connectionManager.setMaxTotal(properties.getMaxTotal());
//        return connectionManager;
//    }
//
//    private ConnectionConfig getConnectionConfig() {
//        return ConnectionConfig.custom()
//                .setBufferSize(properties.getApacheBufferSize())
//                .build();
//    }
//
//    private RequestConfig getRequestConfig() {
//        return RequestConfig.custom()
//                .setSocketTimeout(properties.getSocketTimeout())
//                .setConnectTimeout(properties.getConnectionTimeout())
//                .build();
//    }

    private ConnectionKeepAliveStrategy createKeepAliveStrategy() {
        return new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                // in case of errors keep-alive not always works. close connection just in case
                if (httpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    return -1;
                }
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        httpResponse.headerIterator(HTTP.CONN_DIRECTIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    //String value = he.getValue();
                    if (param != null && param.equalsIgnoreCase(HTTP.CONN_KEEP_ALIVE)) {
                        return properties.getKeepAliveTimeout();
                    }
                }
                return -1;
            }
        };
    }

}
