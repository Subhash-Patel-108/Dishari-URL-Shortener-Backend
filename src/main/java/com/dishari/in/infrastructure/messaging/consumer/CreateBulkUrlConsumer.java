package com.dishari.in.infrastructure.messaging.consumer;


import com.dishari.in.application.service.UrlService;
import com.dishari.in.domain.entity.ShortUrl;
import com.dishari.in.infrastructure.email.EmailService;
import com.dishari.in.infrastructure.messaging.event.CreateBulkUrlEvent;
import com.dishari.in.web.dto.request.CreateBulkUrlRequest;
import com.dishari.in.web.dto.request.CreateCustomUrlRequest;
import com.dishari.in.web.dto.request.CreateNormalUrlRequest;
import com.dishari.in.web.dto.response.BulkErrorDetail;
import com.dishari.in.web.dto.response.CustomUrlResponse;
import com.dishari.in.web.dto.response.NormalUrlResponse;
import com.dishari.in.web.dto.response.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateBulkUrlConsumer {

    private final UrlService urlService ;
    private final EmailService emailService ;

    private String baseUrl ;

    @KafkaListener(
            topics = "create-bulk-url" ,
            groupId = "create-bulk-url-group",
            containerFactory = "bulkKafkaListenerContainerFactory"
    )
    public void consume(@Payload CreateBulkUrlEvent event , @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Consuming message from partition: {} and offset: {}", partition, offset);

        try {
            createBulkUrl(event);
        } catch (Exception ex) {
            log.error("Failed to create bulk url for user={}", event.getUserEmail(), ex);
        }
    }

    private void createBulkUrl(CreateBulkUrlEvent event) {
        int totalProcesses = event.getUrls().size() ;
        List<String> successfulUrls = new ArrayList<>() ;
        List<BulkErrorDetail> failedUrls = new ArrayList<>() ;

        for (CreateNormalUrlRequest request : event.getUrls()) {
            try {
                NormalUrlResponse response = urlService.createNormalUrl(event.getUserEmail() , request) ;
                successfulUrls.add(response.shortUrl()) ;
            }catch (Exception e) {
                failedUrls.add(new BulkErrorDetail(request.originalUrl(), e.getMessage()));
            }
        }

        //To send the bulk url creation report to the user by mail
        emailService.sendBulkUrlReport(event.getUserEmail() , event.getUserName() , successfulUrls , failedUrls);
    }
}
