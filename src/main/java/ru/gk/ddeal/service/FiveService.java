package ru.gk.ddeal.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FiveService {
    private static final Logger log = LoggerFactory.getLogger(FiveService.class);

    private final CheckService checkService;

    @Autowired
    public FiveService(CheckService checkService) {
        this.checkService = checkService;
    }

    public Message requestHandler(Message message) throws Exception {

        final MimeMessageParser parser = new MimeMessageParser((MimeMessage) message);
        parser.parse();
        final String htmlContent = parser.getHtmlContent();
        log.info("Mail body:" + htmlContent);

        final Document html = Jsoup.parse(htmlContent, "", Parser.xmlParser());
        if (html.select("td:contains(Дата)").last() != null) {
            String date = html.select("td:contains(Дата)").last().parent().child(1).text();
            String sum = html.select("td:contains(Итог)").last().parent().child(1).text();

            String txtBlock = html.select("td:contains(ФН)").last().text();

            int s = txtBlock.indexOf("ФН:");
            int e = txtBlock.indexOf(" ", s + 4);
            String fn = txtBlock.substring(s + 4, e);

            s = txtBlock.indexOf("ФД:");
            e = txtBlock.indexOf(" ", s + 4);
            String fd = txtBlock.substring(s + 4, e);

            s = txtBlock.indexOf("ФПД:");
            e = txtBlock.indexOf(" ", s + 5);
            String fpd = txtBlock.substring(s + 5, e);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm");
            log.info("check info date: " + date + " sum:" + sum + " fn:" + fn + " fd:" + fd + " fpd:" + fpd);
            checkService.sendCheck(LocalDateTime.parse(date, formatter), sum, fn, fd, fpd);
        }
        return message;
    }
}
