package paymentengine.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import paymentengine.domain.Transaction;
import paymentengine.dto.TransferRequestDTO;
import paymentengine.dto.TransferResponseDTO;
import paymentengine.service.TransferService;

@RestController
@RequestMapping("api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponseDTO> createTransfer(@RequestBody @Valid TransferRequestDTO request) {

        Transaction transaction = transferService.executeTransfer(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount()
        );

        TransferResponseDTO response = new TransferResponseDTO(transaction);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);


    }


}
