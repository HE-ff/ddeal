package ru.gk.ddeal.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class CheckService {
    private static final Logger log = LoggerFactory.getLogger(CheckService.class);

    @Value("${Authorization}")
    private String authorization;

    @Value("${Duid}")
    private String duid;

    @Value("${Uid}")
    private String uid;

    private static final String CHECK_URL = "aHR0cHM6Ly9jYi5lZGFkZWFsLnJ1L3YyL3VzZXIvY2hlY2tz";
    private static final String DUID = "RWRhZGVhbC1EdWlk";
    private static final String UID = "RWRhZGVhbC1VaWQ=";

    private final RestTemplate restTemplate;
    private static final CustomHttpRequestInterceptor requestInterceptor = new CustomHttpRequestInterceptor();

    @Autowired
    public CheckService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendCheck(final LocalDateTime date, final String sum, final String fn, final String fd, final String fpd) {
        final String checkUrl = new String(Base64.decodeBase64(CHECK_URL.getBytes()));

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

        headers.set("Authorization", authorization);
        headers.set(new String(Base64.decodeBase64(DUID.getBytes())), duid);
        headers.set(new String(Base64.decodeBase64(UID.getBytes())), uid);
        headers.set("X-Platform", "android' -H $'X-OS-Version: 7.1.1");
        headers.set("X-App-Version", "5.16.0");
        headers.set("AmVersion", "7.17.1");
        headers.set("Content-Type", "text/plain; charset=utf-8");

        String s = "t=".concat(date.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")))
                .concat("&s=")
                .concat(sum)
                .concat("&fn=")
                .concat(fn)
                .concat("&i=")
                .concat(fd)
                .concat("&fp=")
                .concat(fpd)
                .concat("&n=1");
        log.info("request body" + s);
        HttpEntity<String> request = new HttpEntity<>(s, headers);
        restTemplate.setInterceptors(Collections.singletonList(requestInterceptor));
        ResponseEntity<String> result = restTemplate.postForEntity(checkUrl, request, String.class);

        log.info("Response result " + result.getStatusCode().getReasonPhrase() + ":" + result);
    }

    public static class CustomHttpRequestInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            HttpHeaders headers = request.getHeaders();
            headers.set(HttpHeaders.ACCEPT, null);
            headers.set("User-agent", "okhttp/3.14.0");

            return execution.execute(request, body);
        }
    }
}
