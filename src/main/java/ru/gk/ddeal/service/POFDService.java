package ru.gk.ddeal.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class POFDService {
    private static final Logger log = LoggerFactory.getLogger(POFDService.class);

    private final CheckService checkService;

    public POFDService(CheckService checkService) {
        this.checkService = checkService;
    }

    public Message requestHandler(Message message) throws Exception {

        if (Arrays.stream(message.getFrom()).map(i -> ((InternetAddress) i).getAddress()).anyMatch(i -> i.contains("noreply@chek.pofd.ru"))) {
            final MimeMessageParser parser = new MimeMessageParser((MimeMessage) message);
            parser.parse();
            final String htmlContent = parser.getHtmlContent();

            final Document html = Jsoup.parse(htmlContent, "", Parser.xmlParser());

            final String date = html.select("div:matches((0[1-9]|[12][0-9]|3[01])[.](0[1-9]|1[012])[.](19|20)\\d\\d ([0-9][0-9]:[0-9][0-9]))").last().text().trim();
            final String sum = html.select("div:contains(ИТОГ)").last().parent().getElementsMatchingText("\\d").last().text().trim();
            final String fn = html.select("div:contains(ФН)").last().parent().getElementsMatchingText("\\d").last().text().trim();
            final String fd = html.select("div:contains(N ФД)").last().parent().getElementsMatchingText("\\d").last().text().trim();
            final String fpd = html.select("div:contains(ФП)").last().parent().getElementsMatchingText("\\d").last().text().trim();

            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            log.info("check info date:" + date + " sum:" + sum + " fn:" + fn + " fd:" + fd + " fpd:" + fpd);
            checkService.sendCheck(LocalDateTime.parse(date, formatter), sum, fn, fd, fpd);
        }

        return message;

    }


}
