package org.apache.spark.sql.mlsql.sources.mysql.binlog.io;


import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import org.apache.spark.sql.mlsql.sources.mysql.binlog.MySQLCDCUtils;
import org.apache.spark.sql.mlsql.sources.mysql.binlog.RawBinlogEvent;
import org.apache.spark.sql.mlsql.sources.mysql.binlog.TableInfo;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class UpdateRowsWriter extends AbstractEventWriter {


    @Override
    public List<String> writeEvent(RawBinlogEvent event) {
        UpdateRowsEventData data = event.getEvent().getData();
        List<String> items = new ArrayList<>();

        for (Map.Entry<Serializable[], Serializable[]> row : data.getRows()) {
            try {
                StringWriter writer = new StringWriter();
                startJson(writer, event);
                final BitSet bitSet = data.getIncludedColumns();
                writeRow(event.getTableInfo(), row, bitSet);
                endJson();
                items.add(writer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return items;
    }

    protected void writeRow(TableInfo tableInfo, Map.Entry<Serializable[], Serializable[]> row, BitSet includedColumns) throws IOException {

        jsonGenerator.writeArrayFieldStart("rows");
        int i = includedColumns.nextSetBit(0);
        jsonGenerator.writeStartObject();

        while (i != -1) {
            String columnName = new SchemaTool(tableInfo.getSchema()).getColumnNameByIndex(i);
            Serializable[] newRow = row.getValue();
            if (newRow != null) {
                jsonGenerator.writeObjectField(columnName, MySQLCDCUtils.getWritableObject(newRow[i]));
            }
            i = includedColumns.nextSetBit(i + 1);
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndArray();
    }
}
