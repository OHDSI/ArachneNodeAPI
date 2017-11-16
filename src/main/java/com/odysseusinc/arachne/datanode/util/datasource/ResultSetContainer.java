package com.odysseusinc.arachne.datanode.util.datasource;

import java.util.List;

public class ResultSetContainer<R> {

    R values;
    List defaultValue;

    public ResultSetContainer(R values, List defaultValue) {

        this.values = values;
        this.defaultValue = defaultValue;
    }

    public R getValues() {

        return values;
    }

    public List getDefaultValue() {

        return defaultValue;
    }
}
