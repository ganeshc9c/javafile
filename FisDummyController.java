package in.kitecash.tab.cardservice.controller;

import in.kitecash.tab.cardservice.dto.fis.*;
import in.kitecash.tab.cardservice.fis.service.FisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@Slf4j
public class FisDummyController {

    private FisService fisService;


    @Autowired
    public FisDummyController(FisService fisService) {
        this.fisService = fisService;
    }

    @PostMapping(value = "/fis/dummy/cardordernonperso", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> orderNonPersoCards (@RequestBody NonPersoCardOrderDTO nonPersoCardOrderDTO) {
        return fisService.orderNonPersoCards(nonPersoCardOrderDTO);
    }

    @PostMapping(value = "/fis/dummy/cardordercancel", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> cancelNonPersoCards (@RequestBody CancelNonPersoCardsDTO cancelNonPersoCardsDTO){
        return fisService.cancelNonPersoCards(cancelNonPersoCardsDTO);
    }

    @PostMapping(value = "/fis/dummy/cardorderenq", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> enquireNonPersoCards (@RequestBody EnquireNonPersoCardsDTO enquireNonPersoCardsDTO){
        return fisService.enquireNonPersoCards(enquireNonPersoCardsDTO);
    }

    @PostMapping(value = "/fis/dummy/changecardstatus", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> changeStatusCards (@RequestBody CardStatusChangeDTO cardStatusChangeDTO){
        return fisService.changeStatusCards(cardStatusChangeDTO);
    }

    @PostMapping(value = "/fis/dummy/updatecustprofile", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> updateCustomerDetails (@RequestBody UpdateCustomerDetailsDTO updateCustomerDetailsDTO){
        return fisService.updateCustomerDetails(updateCustomerDetailsDTO);
    }

    @PostMapping(value = "/fis/dummy/cardreplacenonperso", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> replaceNonPersoCards (@RequestBody ReplaceNonPersoCardsDTO replaceNonPersoCardsDTO){
        return fisService.replaceNonPersoCards(replaceNonPersoCardsDTO);
    }

    @PostMapping(value = "/fis/dummy/cardlink", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> linkNonPersoCards (@RequestBody LinkNonPersoCardsDTO linkNonPersoCardsDTO){
        return fisService.linkNonPersoCards(linkNonPersoCardsDTO);
    }

    @PostMapping(value = "/fis/dummy/cardproductchng", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> upgradeCards (@RequestBody UpgradeCardsDTO upgradeCardsDTO){
        return fisService.upgradeCards(upgradeCardsDTO);
    }

    @PostMapping(value = "/fis/dummy/cardorderstanperso", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> orderStdPersoCards (@RequestBody StandardPersoCardOrderDTO standardPersoCardOrderDTO){
        return fisService.orderStdPersoCards(standardPersoCardOrderDTO);
    }

    @PostMapping(value = "/fis/dummy/getcustomerdetails", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> getCustomerDetails(@RequestBody CustomerDetailsDTO customerDetailsDTO){
        return fisService.getCustomerDetails(customerDetailsDTO);
    }

    @PostMapping(value = "/fis/dummy/virtualcardcreation", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> virtualCardCreation (@RequestBody VirtualCardCreationDTO virtualCardCreationDTO){
        return fisService.virtualCardCreation(virtualCardCreationDTO);
    }

    @PostMapping(value = "/fis/dummy/virtual2physical", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> virtualToPhysicalCardCreation (@RequestBody VirtualToPhysicalCardCreationDTO virtualToPhysicalCardCreationDTO){
        return fisService.virtualToPhysicalCardCreation(virtualToPhysicalCardCreationDTO);
    }

    @PostMapping(value = "/fis/dummy/adjaccountbalance", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> adjustAccountBalance (@RequestBody AdjustAccountBalanceDTO adjustAccountBalanceDTO){
        return fisService.adjustAccountBalance(adjustAccountBalanceDTO);
    }

    @PostMapping(value = "/fis/dummy/accountload", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> accountLoad (@RequestBody AccountLoadDTO accountLoadDTO){
        return fisService.accountLoad(accountLoadDTO);
    }

    @PostMapping(value = "/fis/dummy/setpin", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> setPin (@RequestBody SetPinDTO setPinDTO){
        return fisService.setPin(setPinDTO);
    }

    @PostMapping(value = "/fis/dummy/changepin", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> changePin (@RequestBody ChangePinDTO changePinDTO){
        return fisService.changePin(changePinDTO);
    }

    @PostMapping(value = "/fis/dummy/verifypin", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> verifyPin (@RequestBody VerifyPinDTO verifyPinDTO){
        return fisService.verifyPin(verifyPinDTO);
    }

    @PostMapping(value = "/fis/dummy/gettransactiondetails", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> getTransactionDetails (@RequestBody TransactionDetailsDTO transactionDetailsDTO){
        return fisService.getTransactionDetails(transactionDetailsDTO);
    }

    @PostMapping(value = "/fis/dummy/giftcardorder", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> giftcardorder (@RequestBody GiftCardOrderDTO giftCardOrderDTO){
        return fisService.giftcardorder(giftCardOrderDTO);
    }

    @PostMapping(value = "/fis/dummy/verifycvv", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> cvvVerification (@RequestBody CvvVerificationDTO cvvVerificationDTO){
        return fisService.cvvVerification(cvvVerificationDTO);
    }

    @PostMapping(value = "/fis/dummy/carddetails", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> getCardDetails (@RequestBody CardDetailsDTO cardDetailsDTO){
        return fisService.getCardDetails(cardDetailsDTO);
    }

    @PostMapping(value = "/fis/dummy/channelupdate", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> updateChannel (@RequestBody UpdateChannelDTO updateChannelDTO){
        return fisService.updateChannel(updateChannelDTO);
    }

    @PostMapping(value = "/fis/dummy/cardlimit", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> updateLimit (@RequestBody UpdateLimitDTO updateLimitDTO){
        return fisService.updateLimit(updateLimitDTO);
    }

    @PostMapping(value = "/fis/dummy/cardlimitandchannel", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    Map<String, Object> updateLimitAndChannel (@RequestBody UpdateLimitAndChannelDTO updateLimitAndChannelDTO){
        return fisService.updateLimitAndChannel(updateLimitAndChannelDTO);
    }

}