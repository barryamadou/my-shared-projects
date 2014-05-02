package com.karavel.tutorial.conf;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.karavel.tutorial.indexation.IndexerMananager;
import com.karavel.tutorial.rest.IndexerMananagerOut;


public class SplunkKpiWritter {

    private StringBuilder sb;
    private final char DATA_SEPARATOR = ' ';
    private final char KEY_VALUE_SEPARATOR = '=';
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    public SplunkKpiWritter(String kpi) {
        sb = new StringBuilder("kpi").append(KEY_VALUE_SEPARATOR).append(kpi);
    }

    public SplunkKpiWritter data(String key, Integer value) {
        sb.append(DATA_SEPARATOR).append(key).append(KEY_VALUE_SEPARATOR).append(value);
        return this;
    }

    public SplunkKpiWritter data(String key, String value) {
        sb.append(DATA_SEPARATOR).append(key).append(KEY_VALUE_SEPARATOR).append(value);
        return this;
    }

    public SplunkKpiWritter data(String key, Date value) {
        sb.append(DATA_SEPARATOR).append(key).append(KEY_VALUE_SEPARATOR).append(dateFormatter.format(value));
        return this;
    }

    public SplunkKpiWritter data(String key, Long value) {
        sb.append(DATA_SEPARATOR).append(key).append(KEY_VALUE_SEPARATOR).append(value);
        return this;
    }
    public SplunkKpiWritter datas(Map<String, Object> datas) {
    	for(Entry<String, Object> data : datas.entrySet()) {
    		String key = data.getKey();
    		Object value = data.getValue();
    		if (value instanceof Date) {
    			data(key, (Date) value);
    		} else {
    			data(key, value.toString());
    		}
    	}
    	return this;
    }
    
    @Override
    public String toString() {
        return sb.toString();
    }
}