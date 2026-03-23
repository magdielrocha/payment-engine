package paymentengine.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import paymentengine.dto.TransferRequestDTO;
import paymentengine.dto.TransferResponseDTO;
import paymentengine.security.CustomUserDetails;
import paymentengine.service.TransferService;

@RestController
@RequestMapping("api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponseDTO> transfer(
            @RequestBody @Valid TransferRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long loggedUserId = userDetails.getUser().getId();

        var transaction = transferService.executeTransfer(
                loggedUserId,
                request.destinationAccountId(),
                request.amount()
        );

        TransferResponseDTO response = new TransferResponseDTO(
                transaction.getId(),
                transaction.getSourceAccount().getId(),
                transaction.getDestinationAccount().getId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
