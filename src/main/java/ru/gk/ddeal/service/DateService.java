package ru.gk.ddeal.service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service

public class DateService {

    private DateFile dateFile;
    private final ObjectMapper objectMapper;
    private final Resource resourceFile;
    private static final Logger log = LoggerFactory.getLogger(DateService.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Autowired
    public DateService(@Value("file:${datefile}") final Resource resourceFile, final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.resourceFile = resourceFile;
    }

    @PostConstruct
    private void readOrCreateDateFile() throws IOException {

        if (resourceFile.exists()) {
            dateFile = this.objectMapper.readValue(resourceFile.getFile(), DateFile.class);
        } else {
            dateFile = new DateFile(Date.from(LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now()))));
            this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            objectMapper.writeValue(new File(resourceFile.getFilename()), dateFile);
        }
    }


    public SearchTerm dateAfterTerm(Flags supportedFlags, Folder folder) {
        return new ReceivedDateTerm(ComparisonTerm.GT, dateFile.lastDate);
    }


    public void dateHadler(Message message) {

        try {
            final Date recieveDate = message.getReceivedDate();

            if (recieveDate.after(dateFile.lastDate)) {
                dateFile.lastDate = recieveDate;
                objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
                objectMapper.writeValue(new File(resourceFile.getFilename()), dateFile);
                log.info("Last date: " + DATE_FORMAT.format(recieveDate));
            }
        } catch (IOException | MessagingException e) {
            log.error("dateHadler:", e);
        }

    }


    private static class DateFile {
        @JsonProperty(required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
        private Date lastDate;

        @JsonCreator
        public DateFile(@JsonProperty(value = "lastDate", required = true) Date lastDate) {
            this.lastDate = lastDate;
        }

    }
}
