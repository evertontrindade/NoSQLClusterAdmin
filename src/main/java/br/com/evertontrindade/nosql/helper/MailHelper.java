package br.com.evertontrindade.nosql.helper;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Created by everton on 01/10/16.
 */
@Component
public class MailHelper {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private MessageSource messageSource;
    
    public void send(String to, String subject, String text, String... args) throws MessagingException, UnsupportedEncodingException {
        if (!StringUtils.isEmpty(to)) {
        	Locale locale = LocaleContextHolder.getLocale();
        	
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false);
            mimeMessageHelper.setTo(to.split(","));
            mimeMessageHelper.setFrom(new InternetAddress("monitor.nosql.app@gmail.com", "Alerta de Monitoramento - NoSQL"));
            mimeMessageHelper.setSubject(messageSource.getMessage(subject, null, locale));
            mimeMessageHelper.setText(messageSource.getMessage(text, args, locale), true);
            javaMailSender.send(mimeMessage);
        }
    }

}
