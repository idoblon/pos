package com.springboot.POS;

import com.springboot.POS.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionBackfillRunner implements ApplicationRunner {

    private final StoreService storeService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = storeService.backfillSubscriptionDates();
            if (count > 0) {
                System.out.println("✅ Backfilled subscription dates for " + count + " existing stores.");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Subscription backfill failed: " + e.getMessage());
        }
    }
}
