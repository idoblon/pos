package com.springboot.POS;

import com.springboot.POS.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionBackfillRunner.class);

    private final StoreService storeService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = storeService.backfillSubscriptionDates();
            if (count > 0) {
                log.info("Backfilled subscription dates for {} existing stores.", count);
            }
        } catch (Exception e) {
            log.error("Subscription backfill failed: {}", e.getMessage());
        }
    }
}
