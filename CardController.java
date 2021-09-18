package in.kitecash.tab.cardservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.kitecash.tab.cardservice.dto.*;
import in.kitecash.tab.cardservice.dto.fis.NonPersoCardOrderDTO;
import in.kitecash.tab.cardservice.dto.pnb.credit.VerifyCreditCardUserOTPRequest;
import in.kitecash.tab.cardservice.dto.yap.AddCardUserRequestYap;
import in.kitecash.tab.cardservice.dto.yap.CardUserInfo;
import in.kitecash.tab.cardservice.enums.*;
import in.kitecash.tab.cardservice.exception.CardAlreadyRequestedException;
import in.kitecash.tab.cardservice.exception.InvalidInputParamException;
import in.kitecash.tab.cardservice.exception.NoCardOrderFoundException;
import in.kitecash.tab.cardservice.flow.CardFlow;
import in.kitecash.tab.cardservice.matchmove.service.MatchMoveService;
import in.kitecash.tab.cardservice.params.CardOrderRequestParam;
import in.kitecash.tab.cardservice.service.CardService;
import in.kitecash.tab.common.dto.SanitizeDTO;
import in.kitecash.tab.common.enums.BankName;
import in.kitecash.tab.common.exception.KiteException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Timestamp;
import java.util.*;

import static in.kitecash.tab.common.util.ConverterUtil.convertValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@Slf4j
public class CardController {

    private static final int BIN_START_INDEX = 0;
    private static final int BIN_END_INDEX = 6;
    private CardService cardService;

    private MatchMoveService matchMoveService;

    private CardFlow cardFlow;

    @Value("${mmove.indusind.bin}")
    private String defaultBin;

    @Autowired
    public CardController(CardService cardService, MatchMoveService matchMoveService, CardFlow cardFlow) {
        this.cardService = cardService;
        this.matchMoveService = matchMoveService; 
        this.cardFlow = cardFlow;
    }

    private static final Logger LOG = LoggerFactory.getLogger(CardController.class);


    @Deprecated
    @PostMapping(value = "/v1/cards/order", consumes = "application/json")
    public CardOrderDTO savePhysicalCardRequest(@RequestBody CardOrderRequestParam requestParam) throws CardAlreadyRequestedException {

        cardService.findByUserIdAndStatus(requestParam.getUserId(), CardOrderStatus.INITIATED);

        CardOrderDTO cardOrderDTO = cardService.saveCardOrderRequest(requestParam);
        return cardOrderDTO;
    }

    @GetMapping(value = "/v1/cards/user/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CardUserDTO> getCardUsersByCreationStatus(@PathVariable(value = "status") CardUserCreationStatus status) {

        return cardService.getCardUsersByCreationStatus(status);
    }

    // Method Just fullyRegisterCardUser on Matchmove only
    @PutMapping(value = "/v1/cards/user-register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void fullyRegisterCardUserAndCreateCard(@RequestBody Map<String, List> cardUsersMap) {


        List<CardUserDTO> partialCardUsers = getCardUserDTO(cardUsersMap.get("partialCardUsers"));
        List<UserDTO> users = getUserDTOs(cardUsersMap.get("users"));

        partialCardUsers.forEach(partialCardUser -> {
            try {
                if (partialCardUser.getBankName() == BankName.FEDERAL_BANK || partialCardUser.getBankName() == BankName.INDUSIND || partialCardUser.getBankName() == BankName.INDUSIND_JIT) {
                    JSONObject cardUserJson = matchMoveService.getUserInfo(partialCardUser);

                    if (cardUserJson != null) {

                        updateCardUser(partialCardUser, cardUserJson.getString("id"));
                    } else {

                        UserDTO user = users.stream().
                                filter(u -> u.getUserId().equals(partialCardUser.getUserId())).
                                findFirst().get();

                        Map<String, String> userCreationParams = new HashMap<>();
                        userCreationParams.put("email", partialCardUser.getUsername());
                        userCreationParams.put("password", partialCardUser.getPassword());
                        userCreationParams.put("first_name", user.getFirstName());
                        userCreationParams.put("last_name", user.getLastName());
                        userCreationParams.put("preferred_name", StringUtils.left(user.getFirstName(), 25));
                        userCreationParams.put("mobile_country_code", "91");
                        userCreationParams.put("mobile", user.getMobileNo().substring(3));
                        if(Objects.nonNull(partialCardUser.getBankName()))
                            userCreationParams.put("bank_name", partialCardUser.getBankName().name());


                        cardUserJson = matchMoveService.createCardUser(userCreationParams);

                        updateCardUser(partialCardUser, cardUserJson.getString("id"));
                    }
                }
            } catch (Exception e) {
                log.error("cardUser could not be registered with id " + partialCardUser.getUserId(), e);
            }
        });
    }


    private void updateCardUser(CardUserDTO cardUser, String id) {

        try {
            cardUser.setVirtualUserId(id); //  setting it explicitly for now till refactoring
            //Commenting so that CRON does not create DIGITAL card for PARTIAL users
            /*JSONObject vCardJSON = matchMoveService.createCard(cardUser.getEmail(), cardUser.getLoginKey());
            cardService.createCard(cardUser, vCardJSON.getString("id"), SYSTEM, CardType.DIGITAL);*/
            cardService.updateCardUser(cardUser, id);
        } catch (Exception e) {
            LOG.error("Card Creation Failed For User : " + cardUser.getUserId() + " ", e);
        }
    }


    private List<UserDTO> getUserDTOs(List<? extends Object> users) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(users, new TypeReference<List<UserDTO>>() {
        });
    }

    private List<CardUserDTO> getCardUserDTO(List<? extends Object> objects) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(objects, new TypeReference<List<CardUserDTO>>() {
        });
    }

    @GetMapping(path = "/v1/cards/{cardId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CardDTO getCardByCardId(@PathVariable(value = "cardId") String cardId,
                                   @RequestParam(value = "status") List<CardStatus> cardStatuses) throws KiteException {

        if (CollectionUtils.isEmpty(cardStatuses)) {
            cardStatuses = Arrays.asList(CardStatus.ACTIVE, CardStatus.INACTIVE);
        }
        return cardService.getCardByCardId(cardId, cardStatuses);
    }

    @PostMapping(path = "/v1/cards", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CardDTO> getCardsByUserIds(@RequestBody UserIdListDTO userIdListDTO) {
        return cardService.getCardsByUserIds(userIdListDTO.getUserIds(), userIdListDTO.getCompanyCode());
    }

    @Deprecated
    @GetMapping("/v1/card-orders/mine")
    public CardOrderDTO getCardOrderByUserId(@RequestParam("userId") String userId, @RequestParam("enterpriseId")
            String enterpriseId) throws NoCardOrderFoundException {

        return cardService.getCardOrder(userId, enterpriseId);
    }

    @Deprecated
    @PostMapping(value = "/v1/cards/exists", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public CardExists cardExists(@RequestBody CardParams cardParams) {
        return cardService.cardExists(cardParams);
    }

    @GetMapping(path = "/heartbeat")
    public void heartbeat() {
    }

    @PostMapping(path = "/v1/card/physical", consumes = APPLICATION_JSON_VALUE)
    public void addPhysicalCard(@RequestBody AddPhysicalCardRequest request) throws Exception {

        String bin = defaultBin;

        if(Objects.nonNull(request.getCardNumber()))
              bin=  request.getCardNumber().substring(BIN_START_INDEX, BIN_END_INDEX);

        BinDetailsDTO binDetailsDTO = cardService.getActiveBinDetails(bin);
        request.setBin(bin);
        request.setBankName(binDetailsDTO.getBankName());
        request.setBankProgram(BankProgram.valueOf(binDetailsDTO.getCardCategory().name()));
        request.setNetwork(binDetailsDTO.getNetwork());

        cardFlow.addPhysicalCard(request);
    }

    @PostMapping(path = "/v1/users/add-funds", consumes = APPLICATION_JSON_VALUE)
    public void addFunds(@RequestBody AddFundsToCardsDTO request) {
        cardFlow.addFunds(request);
    }

    @Deprecated
    @GetMapping(path = "/v1/user/card/{userId}", produces = APPLICATION_JSON_VALUE)
    public CardInfoDTO getCardDetail(@PathVariable("userId") String userId) throws Exception {
        return cardFlow.getCardInfo(userId);
    }

    @GetMapping(path = "/v1/card/{cardId}", produces = APPLICATION_JSON_VALUE)
    public CardInfoDTO getCardDetailfromcardId(@PathVariable("cardId") String cardId) throws Exception {
        return cardFlow.getCardInfofromcardId(cardId);
    }

    // TODO: It should be removed and migrated completely to v2/cards/add-card
    @PostMapping(path = "/v1/cards/add-card", consumes = APPLICATION_JSON_VALUE)
    public void addCardUserAndPhysicalCard(@RequestBody PhysicalCardRequest request) throws Exception {

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

        cardFlow.addCardUserAndPhysicalCard(request);
    }

    @Deprecated
    @GetMapping(path = "/v1/cards/users/{userId}", produces = APPLICATION_JSON_VALUE)
    public List<CardInfoDTO> getCardDetail(@PathVariable("userId") String userId,
                                           @RequestParam(value = "programName", required = false) String programName) throws Exception {

        //TODO:: for now hardCode cardType, BankName and BankProgram
        return cardFlow.getCardInfoForUserId(userId, programName,CardType.PHYSICAL, BankName.FEDERAL_BANK, BankProgram.PREPAID);
    }

    @Deprecated
    @GetMapping(path = "/v1/cards/card-basic/{userId}", produces = APPLICATION_JSON_VALUE)
    public List<CardDTO> getCardDTOForUserId(@PathVariable("userId") String userId, @RequestParam(value = "programName", required = false) String programName) {

        return cardFlow.getCardDTOForUserId(userId, programName);
    }

    @Deprecated
    @PostMapping(path = "/v1/cards/add-fund", consumes = APPLICATION_JSON_VALUE)
    public void loadFund(@RequestBody CardFundDTO request) {

        CardLoadFundDTO cardLoadFundDTO = convertValue(request, CardLoadFundDTO.class);

        cardFlow.loadFund(cardLoadFundDTO);
    }

    // Now bankName will be mandatory for userKyc method
    @PutMapping(path = "/v1/users/{userId}", consumes = APPLICATION_JSON_VALUE)
    public void updateUser(@PathVariable("userId") String userId,
                           @RequestParam(value = "bankName") BankName bankName,
                           @RequestBody UserKycDTO userKycDTO) {
        cardFlow.userKYC(userId, Optional.ofNullable(bankName).orElseThrow(InvalidInputParamException::new),
                userKycDTO.getUpdateUserDTO(), userKycDTO.getUserAddressDTO());
    }

    @GetMapping(path = "/v1/users/kyc-status", produces = APPLICATION_JSON_VALUE)
    public List<KycRequestStatusDTO> getFullKycStatusByUserIdIn(@RequestParam("userIds") List<String> userIds, @RequestParam("bankName") BankName bankName) {
        return cardFlow.getFullKycStatusByUserIdIn(userIds, bankName);
    }

    // Filters based on Card UUID and Card Status Active
    @GetMapping(path = "/v1/cards/uuid/{id}" , produces= APPLICATION_JSON_VALUE)
    public CardDTO getActiveCardById(@PathVariable(value = "id") String id) {
        
        
        
        wefnkjfbiufbyufbefbf
        return cardService.getCardById(id);
    }

    /**
     * TODO: Send Bankname and Bankprogram params in AddCardUserRequest from calling layers
     * Default are {@link in.kitecash.tab.common.enums.BankName#FEDERAL_BANK}
     * and {@link in.kitecash.tab.cardservice.enums.BankProgram#PREPAID}
     * @param addCardUserRequest
     */
    @PostMapping(path = "/v1/users", consumes = APPLICATION_JSON_VALUE)
    public CardUserDTO addUser(@RequestBody AddCardUserRequest addCardUserRequest) {
        return cardFlow.addUser(addCardUserRequest);
    }

    @GetMapping(path = "v1/user-ids/filter", produces = APPLICATION_JSON_VALUE)
    Set<String> filterUsers(@RequestParam(value = "programName") String programName,
                            @RequestParam(value = "bankName", required = false) BankName bankName,
                            @RequestParam(value = "cardUserCreationStatus", required = false) CardUserCreationStatus cardUserCreationStatus,
                            @RequestParam(value = "cardStatus", required = false) CardStatus cardStatus,
                            @RequestParam(value = "cardUsageStatus", required = false) CardUsageStatus cardUsageStatus,
                            @RequestParam(value = "network", required = false) Network network,
                            @RequestParam("userIds") Set<String> userIds) {
        return cardFlow.filterUsers(programName, bankName, cardUserCreationStatus, cardStatus, cardUsageStatus, network, userIds);
    }

    @GetMapping(path = "v1/users/{userId}", produces = APPLICATION_JSON_VALUE)
    public CardUserDTO getUserByUserId(@PathVariable("userId") String userId, @RequestParam("bankName") BankName bankName) {
        return cardFlow.getUserByUserId(userId, bankName);
    }

    @Deprecated
    @PatchMapping(path = "v1/users/{userId}", consumes = APPLICATION_JSON_VALUE)
    public void updateUserByUserId(@PathVariable("userId") String userId, @RequestBody UpdateUserRequestDTO updateUserRequestDTO) {
        cardFlow.updateUserByUserId(userId, updateUserRequestDTO);
    }

    @GetMapping(path = "/v1/user-cards", produces = APPLICATION_JSON_VALUE)
    public List<CardDTO> getUserCardsByUpdated(@RequestParam(value = "programName") String programName,
                                        @RequestParam("enterpriseId") String enterpriseId,
                                        @RequestParam("updated") Timestamp updated){

        return cardFlow.getUserCardsByUpdated(enterpriseId, programName, updated);
    }

    @PatchMapping(value = "v1/user/{userId}", consumes = APPLICATION_JSON_VALUE)
    public void updateUser(@PathVariable("userId") String userId, @RequestBody UpdateCardUserRequest updateCardUserRequest) {
        cardFlow.updateUser(userId, updateCardUserRequest);
    }

    @PostMapping(value = "v1/user/wallet", consumes = APPLICATION_JSON_VALUE)
    public void createWallet(@RequestBody CreateWalletRequest createWalletRequest) {
        cardFlow.createWallet(createWalletRequest);
    }

    @DeleteMapping("v1/cards/{id}/funds")
    public void moveAllFundsFromCardToWallet(@PathVariable("id") String id) {
        cardFlow.moveAllFundsFromCardToWallet(id);
    }

    // Only for Isg Credit Card Users
    @PostMapping(path = "v1/users/verify-otp", consumes = APPLICATION_JSON_VALUE)
    public void verifyCreditCardUserOTP(@RequestBody VerifyCreditCardUserOTPRequest request) {
        cardFlow.verifyCreditCardUserOTP(request);
    }

    @PostMapping(path = "/v1/yap/users", consumes = APPLICATION_JSON_VALUE)
    public CardUserInfo addUser(@RequestBody AddCardUserRequestYap addCardUserRequestYAP){
        return cardFlow.addUser(addCardUserRequestYAP);
    }

    @PostMapping(path = "/v1/cards/set-pin", consumes = APPLICATION_JSON_VALUE)
    public void setPin(@RequestBody SetPinDTO setPinDTO) throws Exception{
         cardFlow.setPin(setPinDTO);
    }

    /**
     * Send UserDTO and BankName params & this gets cardUserDTO or creates it including creating user on MatchMove without creating card
     * {@link in.kitecash.tab.common.enums.BankName#FEDERAL_BANK}
     * @param bankName
     */
    @PostMapping(path = "/v1/card-users", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public CardUserDTO getOrCreateCardUser(@RequestBody UserDTO userDTO, @RequestParam BankName bankName) throws Exception {
        return cardFlow.getOrCreateCardUser(userDTO, bankName);
    }

    @GetMapping(path = "v1/cards/proxy-number/{proxyNumber}", produces = APPLICATION_JSON_VALUE)
    public List<CardDTO> getActiveCardsByProxyNumber(@PathVariable("proxyNumber") String proxyNumber) {
        return cardFlow.getActiveCardsByProxyNumber(proxyNumber);
    }

    @PostMapping(path = "/v1/virtual-cards", consumes = APPLICATION_JSON_VALUE)
    public CardDTO activateVirtualCard(@RequestBody CreateVirtualCardDTO createVirtualCardDTO) {
        return cardFlow.activateVirtualCard(createVirtualCardDTO);
    }

    @GetMapping(path = "/v1/virtual-cards/cvv", produces = APPLICATION_JSON_VALUE)
    public SanitizeDTO getCardCvv(@RequestParam String cardId){
           return new SanitizeDTO(cardFlow.getCardCvv(cardId));
    }

    @PostMapping(path = "/v1/non-perso-cards", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public Map<String, Object> createNonPersoCards(@RequestBody NonPersoCardOrderDTO nonPersoCardOrderDTO) throws
            BadPaddingException,
            IllegalBlockSizeException,
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            KeyStoreException,
            IOException,
            UnrecoverableKeyException,
            CertificateException,
            SignatureException{
        return cardFlow.createNonPersoCards(nonPersoCardOrderDTO);
    }

}
