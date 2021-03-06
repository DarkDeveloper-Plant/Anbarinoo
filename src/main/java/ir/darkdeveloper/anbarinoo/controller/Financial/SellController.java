package ir.darkdeveloper.anbarinoo.controller.Financial;

import ir.darkdeveloper.anbarinoo.dto.FinancialDto;
import ir.darkdeveloper.anbarinoo.dto.SellDto;
import ir.darkdeveloper.anbarinoo.dto.mapper.BuySellMapper;
import ir.darkdeveloper.anbarinoo.model.SellModel;
import ir.darkdeveloper.anbarinoo.service.Financial.SellService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/api/category/products/sell")
@RequiredArgsConstructor
public class SellController {

    private final SellService service;
    private final BuySellMapper mapper;

    @PostMapping("/save/")
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public ResponseEntity<SellDto> saveSell(@RequestBody SellModel sell, HttpServletRequest request) {
        return new ResponseEntity<>(mapper.sellToDto(service.saveSell(Optional.ofNullable(sell), request)),
                HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}/")
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public ResponseEntity<SellDto> updateSell(@RequestBody SellModel sell, @PathVariable Long id,
                                              HttpServletRequest request) {
        return ResponseEntity.ok(mapper.sellToDto(service.updateSell(Optional.ofNullable(sell), id, request)));
    }

    @GetMapping("/get-by-product/{id}/")
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public ResponseEntity<Page<SellDto>> getAllSellRecordsOfProduct(
            @PathVariable("id") Long productId,
            HttpServletRequest request, Pageable pageable) {
        return ResponseEntity.ok(service.getAllSellRecordsOfProduct(productId, request, pageable)
                .map(mapper::sellToDto));
    }

    @GetMapping("/get-by-user/{id}/")
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public ResponseEntity<Page<SellDto>> getAllSellRecordsOfUser(
            @PathVariable("id") Long userId, HttpServletRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(service.getAllSellRecordsOfUser(userId, request, pageable)
                .map(mapper::sellToDto));
    }

    @PostMapping("/get-by-product/date/{id}/")
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public ResponseEntity<Page<SellDto>> getAllSellRecordsOfProductFromDateTo(
            @PathVariable("id") Long productId,
            @RequestBody FinancialDto financial,
            HttpServletRequest req, Pageable pageable) {
        return ResponseEntity.ok(service.getAllSellRecordsOfProductFromDateTo(productId,
                Optional.ofNullable(financial), req, pageable).map(mapper::sellToDto));
    }

    @PostMapping("/get-by-user/date/{id}/")
    public ResponseEntity<Page<SellDto>> getAllSellRecordsOfUserFromDateTo(
            @PathVariable("id") Long userId,
            @RequestBody FinancialDto financial,
            HttpServletRequest req, Pageable pageable) {
        return ResponseEntity.ok(service.getAllSellRecordsOfUserFromDateTo(userId,
                Optional.ofNullable(financial), req, pageable).map(mapper::sellToDto));
    }


    @GetMapping("/{id}/")
    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public ResponseEntity<SellDto> getSell(@PathVariable("id") Long sellId, HttpServletRequest request) {
        return ResponseEntity.ok(mapper.sellToDto(service.getSell(sellId, request)));
    }

    @DeleteMapping("/{id}/")
    public ResponseEntity<?> deleteSell(@PathVariable("id") Long sellId, HttpServletRequest request) {
        return service.deleteSell(sellId, request);
    }

}
