package in.kitecash.tab.cardservice.controller;

import in.kitecash.tab.cardservice.dto.*;
import in.kitecash.tab.cardservice.enums.BankProgram;
import in.kitecash.tab.cardservice.enums.CardBlockingReason;
import in.kitecash.tab.cardservice.flow.CardFlow;
import in.kitecash.tab.cardservice.service.CardService;
import in.kitecash.tab.common.exception.KiteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@Slf4j
public class CardControllerV2 {

    @Value("${mmove.indusind.bin}")
    private String defaultBin;

    private CardService cardService;

    private CardFlow cardFlow;

    @Autowired
    public CardControllerV2(CardService cardService, CardFlow cardFlow) {
        this.cardService = cardService;
        this.cardFlow = cardFlow;
    }

    // Filters based on Card UUID Card Status can be Inactive too
    @GetMapping(path = "/v2/cards/uuid/{id}" , produces= APPLICATION_JSON_VALUE)
    public CardDTO getAnyCardById(@PathVariable(value = "id") String id) {
        return cardService.getAnyCardById(id);
    }

    @GetMapping(path = "/v1/cards/uuid/{id}/info" , produces= APPLICATION_JSON_VALUE)
    public CardInfoDTO getCardInfoById(@PathVariable(value = "id") String id) {
        return cardFlow.getCardInfoById(id);
    }

    // Get All Cards for User
    @GetMapping(path = "/v2/{userId}/cards", produces = APPLICATION_JSON_VALUE)
    public List<CardDTO> getUserCards(@PathVariable("userId") String userId,
                                      @RequestParam(value = "programName") String programName,
                                      @RequestParam("enterpriseId") String enterpriseId) {

        return cardService.getUserCards(userId, enterpriseId, programName);
    }


    @PostMapping(path = "/v2/cards/add-fund", consumes = APPLICATION_JSON_VALUE)
    public void loadFundV2(@RequestBody CardFundDTOV2 cardFundDTOV2){

        cardFlow.loadFundV2(cardFundDTOV2, true);
    }

    @PutMapping(path = "/v1/cards/block", produces = APPLICATION_JSON_VALUE)
    public CardDTO blockCard(@RequestParam("blockedBy") String blockedBy, @RequestParam("id") String id,
                             @RequestParam("reason") CardBlockingReason reason, @RequestParam("enterpriseId") String enterpriseId) {
        return cardFlow.blockCard(blockedBy, id, reason, enterpriseId);
    }

    /**
     * TODO: Refactor in Calling Layers of this endpoint
     * Removing Support for this method by changing the mapping from v1/users/pin to vx/users/pin
     * Need to change in calling microservice of this endpoint and do resetPinById
     */
    @Deprecated
    @PostMapping(path = "/v1/users/pin", consumes = APPLICATION_JSON_VALUE)
    public void resetPin(@RequestBody ChangePinRequestDTO changePinRequest) throws Exception {
        cardFlow.resetPin(changePinRequest);
    }

    @PostMapping(path = "/v1/cards/reset-pin" , consumes = APPLICATION_JSON_VALUE)
    public void resetCardPinByCardId(@RequestBody ResetCardPinDTO resetCardPinDTO) {
        cardFlow.resetPinByCardId(resetCardPinDTO);
    }

    @PatchMapping(value = "/v1/cards", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public CardDTO updateCardUsageStatus(@RequestBody UpdateCardUsageStatusRequest updateCardUsageStatusRequest) throws
            KiteException {

        return cardFlow.updateCardUsageStatusByCardId(updateCardUsageStatusRequest);
    }

    @PostMapping(path = "/v1/cards/activate", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public void activateCardByProxyNumber(@RequestBody CardActivationByProxyNumberRequestDTO cardActivationByProxyNumberRequestDTO) {

        BinDetailsDTO binDetailsDTO = cardService.getActiveBinDetails(cardActivationByProxyNumberRequestDTO.getBin());
        cardActivationByProxyNumberRequestDTO.setBankName(binDetailsDTO.getBankName());
        cardActivationByProxyNumberRequestDTO.setBankProgram(BankProgram.valueOf(binDetailsDTO.getCardCategory().name()));
        cardActivationByProxyNumberRequestDTO.setNetwork(binDetailsDTO.getNetwork());
        cardFlow.activateCardByProxyNumber(cardActivationByProxyNumberRequestDTO);
    }

    @PostMapping(path = "/v2/cards/funds/transfer", consumes = APPLICATION_JSON_VALUE)
    public void cardToCardTransfer(@RequestBody CardToCardDTO cardToCardDTO){

        cardFlow.cardToCardTransfer(cardToCardDTO);
    }

    @PostMapping(path = "/v1/cards/initiate", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public CardDTO initiateCardByProxyNumber(@RequestBody CardInitiationByProxyNumberRequestDTO cardInitiationByProxyNumberRequestDTO) {
        BinDetailsDTO binDetailsDTO = cardService.getActiveBinDetails(cardInitiationByProxyNumberRequestDTO.getBin());
        cardInitiationByProxyNumberRequestDTO.setBankName(binDetailsDTO.getBankName());
        cardInitiationByProxyNumberRequestDTO.setBankProgram(BankProgram.valueOf(binDetailsDTO.getCardCategory().name()));
        cardInitiationByProxyNumberRequestDTO.setNetwork(binDetailsDTO.getNetwork());
        return cardFlow.initiateCardByProxyNumber(cardInitiationByProxyNumberRequestDTO);
    }

    @GetMapping(path = "/v1/cards/rrn/{rrnNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CardDTO getCardByRrnNumber(@PathVariable(value = "rrnNumber") String rrnNumber) throws KiteException {

        return cardService.getCardByRrnNumber(rrnNumber);
    }

    @GetMapping(value = "/v1/bins", produces = APPLICATION_JSON_VALUE)
    public List<BinDetailsDTO> getBinDetails() {
        return cardService.getAllActiveBins();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/v2/cards/add-card", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public CardDTO addCardUserAndPhysicalCard(@RequestBody PhysicalCardRequest request) throws Exception {

        BinDetailsDTO currentBin;

        // TODO: Populate values from calling layers, changes done in API and Prepaid Layer, for additional layers, currently using defaultBin configured via cloud
        if (request.getBankName() != null && request.getNetwork() != null &&
                (currentBin = cardService.getActiveBinDetailsByBankNameAndNetwork(request.getBankName(), request.getNetwork())) != null) {
            request.setBin(currentBin.getBin());
        } else {
            BinDetailsDTO defaultBinDetails = cardService.getActiveBinDetails(defaultBin);
            request.setBankName(defaultBinDetails.getBankName());
            request.setNetwork(defaultBinDetails.getNetwork());
            request.setBin(defaultBin);
        }

        return cardFlow.addCardUserAndPhysicalCard(request);
    }
}
