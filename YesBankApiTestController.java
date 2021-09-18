package in.kitecash.tab.cardservice.controller;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import in.kitecash.tab.cardservice.dto.yesbank.*;
import in.kitecash.tab.cardservice.yesbank.YesBankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("yes-bank-test")
public class YesBankApiTestController {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private YesBankService yesBankService;

    private CheckUserResponse checkUserResponse;
    private CheckKycStatusResponse checkKycStatusResponse;
    private UserRegistrationResponse userRegistrationResponse;
    private VerifyMobileOtpResponse verifyMobileOtpResponse;
    private AadhaarRegistrationResponse aadhaarRegistrationResponse;
    private VerifyAadhaarOtpResponse verifyAadhaarOtpResponse;

    public YesBankApiTestController(Environment environment,
                                    YesBankService yesBankService) {
        log.info("Testing of Yes Bank APIs is enabled only on LOCAL, DEV and QA environments");
        Arrays
                .stream(environment.getActiveProfiles())
                .filter(this::isProfileAllowed)
                .findAny()
                .ifPresent(profile -> {
                    log.info("Allowing testing of Yes Bank APIs on " + profile + " environment");
                    this.yesBankService = yesBankService;
                });
    }

    private boolean isProfileAllowed(String profile) {
        return Lists.newArrayList("local", "dev", "qa").contains(profile.toLowerCase());
    }

    @GetMapping(value = "/check-user/{mobileNumber}", produces = APPLICATION_JSON_VALUE)
    public String checkUser(@PathVariable("mobileNumber") String mobileNumber) {
        checkUserResponse = yesBankService.checkUser(mobileNumber);
        return gson.toJson(checkUserResponse);
    }

    @GetMapping(value = "/check-kyc-status/{mobileNumber}", produces = APPLICATION_JSON_VALUE)
    public String checkKycStatus(@PathVariable("mobileNumber") String mobileNumber) {
        checkKycStatusResponse = yesBankService.checkKycStatus(mobileNumber);
        return gson.toJson(checkKycStatusResponse);
    }

    @PostMapping(value = "/register-user/{mobileNumber}", produces = APPLICATION_JSON_VALUE)
    public String registerUser(@PathVariable("mobileNumber") String mobileNumber) {
        userRegistrationResponse = yesBankService.registerUser(mobileNumber);
        return gson.toJson(userRegistrationResponse);
    }

    @PostMapping(value = "/verify-mobile-otp/{otp}", produces = APPLICATION_JSON_VALUE)
    public String verifyMobileOtp(@PathVariable("otp") String otp) {
        verifyMobileOtpResponse = yesBankService.verifyMobileOtp(userRegistrationResponse.getMobile_number(),
                String.valueOf(userRegistrationResponse.getOtp_ref_number()), otp);
        return gson.toJson(verifyMobileOtpResponse);
    }

    @PostMapping(value = "/register-aadhaar/{aadhaarNumber}", produces = APPLICATION_JSON_VALUE)
    public String registerAadhaar(@PathVariable("aadhaarNumber") String aadhaarNumber) {
        aadhaarRegistrationResponse = yesBankService.registerAadhaar(userRegistrationResponse.getMobile_number(),
                aadhaarNumber, verifyMobileOtpResponse.getToken());
        return gson.toJson(aadhaarRegistrationResponse);
    }

    @PostMapping(value = "/verify-aadhaar-otp/{aadhaarNumber}/{otp}", produces = APPLICATION_JSON_VALUE)
    public String verifyAadhaarOtp(@PathVariable("aadhaarNumber") String aadhaarNumber,
                                   @PathVariable("otp") String otp) {
        verifyAadhaarOtpResponse = yesBankService.verifyAadhaarOtp(userRegistrationResponse.getMobile_number(),
                aadhaarNumber, aadhaarRegistrationResponse.getTransaction_id(), otp, aadhaarRegistrationResponse.getOtp_trans_id());
        return gson.toJson(verifyAadhaarOtpResponse);
    }
}
