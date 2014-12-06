package get.wordy.core.bean.xml;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class TimestampAdapter extends XmlAdapter<String, Timestamp> {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_PATTERN);

    @Override
    public String marshal(Timestamp v) throws Exception {
        Date date = new Date(v.getTime());
        return dateFormat.format(date);
    }

    @Override
    public Timestamp unmarshal(String v) throws Exception {
        Date date = dateFormat.parse(v);
        return new Timestamp(date.getTime());
    }

}