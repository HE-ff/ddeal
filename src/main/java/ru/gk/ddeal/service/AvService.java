package ru.gk.ddeal.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AvService {
    private static final Logger log = LoggerFactory.getLogger(AvService.class);

    private final CheckService checkService;

    public AvService(CheckService checkService) {
        this.checkService = checkService;
    }

    public Message requestHandler(Message message) throws Exception {

        final MimeMessageParser parser = new MimeMessageParser((MimeMessage) message);
        parser.parse();
        final String htmlContent = parser.getHtmlContent();

        Document html = Jsoup.parse(htmlContent, "", Parser.xmlParser());
        if (html.text().contains("Азбука Вкуса")) {

            String date;
            Matcher m = Pattern.compile("(0[1-9]|[12][0-9]|3[01])[.](0[1-9]|1[012])[.](19|20)\\d\\d ([0-9][0-9]:[0-9][0-9])").matcher(html.select("td:contains(Смена №:)").last().text());

            if (m.find()) {
                date = m.group(0);
            } else {
                log.error("no date find" + html.select("td:contains(Смена №:)"));
                throw new IllegalArgumentException("wrong date");
            }

            String sum = html.select("td:contains(Итог стоимость продажи)").last().parent().child(1).text().replace(',', '.').replace(" ", "");
            String fn = html.select("span:contains(№ ФН:)").last().text().replaceAll("[^\\d]", "");
            String fd = html.select("span:contains(№ ФД:)").last().text().replaceAll("[^\\d]", "");
            String fpd = html.select("span:contains(ФПД:)").last().text().replaceAll("[^\\d]", "");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            log.info("check info date:" + date + " sum:" + sum + " fn:" + fn + " fd:" + fd + " fpd:" + fpd);
            checkService.sendCheck(LocalDateTime.parse(date, formatter), sum, fn, fd, fpd);
        }

        return message;

    }


}
