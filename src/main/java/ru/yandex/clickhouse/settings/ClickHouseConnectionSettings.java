package ru.yandex.clickhouse.settings;


public enum ClickHouseConnectionSettings {

    ASYNC("async", false),
    BUFFER_SIZE("buffer_size", 65536),
    APACHE_BUFFER_SIZE("apache_buffer_size", 65536),
    SOCKET_TIMEOUT("socket_timeout", 1200000),
    CONNECTION_TIMEOUT("connection_timeout", 3000),

    /*
    * this is a timeout for data transfer
    * socketTimeout + dataTransferTimeout is sent to ClickHouse as max_execution_time
    * ClickHouse rejects request execution if its time exceeds max_execution_time
    * */
    DATA_TRANSFER_TIMEOUT( "dataTransferTimeout", 10000),


    KEEP_ALIVE_TIMEOUT("keepAliveTimeout", 30 * 1000),

    /**
     * for ConnectionManager
     */
    TIME_TO_LIVE_MILLIS("timeToLiveMillis", 60*1000),
    DEFAULT_MAX_PER_ROUTE("defaultMaxPerRoute", 500),
    MAX_TOTAL("maxTotal", 10000);

    private final String key;
    private final Object defaultValue;
    private final Class clazz;

    ClickHouseConnectionSettings(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.clazz = defaultValue.getClass();
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Class getClazz() {
        return clazz;
    }
}
