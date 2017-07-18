package br.com.evertontrindade.nosql.helper;

import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by everton on 04/09/16.
 */
@Component
public class DateHelper {

    public Date getNextExectution(int minutes) {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MINUTE, minutes);

        return cal.getTime();
    }

}
