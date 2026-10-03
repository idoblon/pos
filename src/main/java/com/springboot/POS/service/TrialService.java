package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.UserDTO;
import com.springboot.POS.payload.response.AuthResponse;

import java.util.Map;

public interface TrialService {

    /** Length of a self-serve free trial. */
    int TRIAL_DAYS = 14;

    /**
     * Starts a 14-day free trial: creates the store (ACTIVE, no payment or
     * admin approval required) plus its store-admin login, and returns an
     * authenticated session so the owner lands straight in the product.
     */
    AuthResponse startTrial(UserDTO userDto) throws UserException;

    /** Trial state for the given user's store (used by the countdown banner). */
    Map<String, Object> getTrialStatus(User user);

    /**
     * Converts a trialing store to paid (called when its subscription payment
     * is approved). No-op for non-trial stores.
     */
    void convertTrial(Long storeId, String plan);

    /** Nightly job: lapses trials past their end date. */
    void expireDueTrials();
}
