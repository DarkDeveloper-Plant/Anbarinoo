package ir.darkdeveloper.anbarinoo.service.Financial;

import ir.darkdeveloper.anbarinoo.exception.BadRequestException;
import ir.darkdeveloper.anbarinoo.exception.ForbiddenException;
import ir.darkdeveloper.anbarinoo.exception.InternalServerException;
import ir.darkdeveloper.anbarinoo.exception.NoContentException;
import ir.darkdeveloper.anbarinoo.model.Financial.BuyModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.repository.Financial.BuyRepo;
import ir.darkdeveloper.anbarinoo.service.ProductService;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class BuyService {

    private final BuyRepo repo;
    private final JwtUtils jwtUtils;
    private final ProductService productService;

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public BuyModel saveBuy(BuyModel buy, HttpServletRequest req) {
        try {
            if (buy.getProduct() == null || buy.getProduct().getId() == null)
                throw new BadRequestException("Product id is null, Can't sell");
            if (buy.getId() != null)
                throw new BadRequestException("Id must be null to save a buy record");
            addProductCount(buy, req);
            return repo.save(buy);
        } catch (NoContentException f) {
            throw new NoContentException(f.getLocalizedMessage());
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public BuyModel updateBuy(BuyModel buy, Long buyId, HttpServletRequest req) {
        try {
            if (buy.getId() != null)
                throw new BadRequestException("Buy id should null for body");

            var preBuyOpt = repo.findById(buyId);
            if (preBuyOpt.isPresent()) {
                preBuyOpt.get().update(buy);
                addProductCount(preBuyOpt.get(), req);
                return repo.save(preBuyOpt.get());
            }

            throw new NoContentException("Buy record do not exist.");
        } catch (NoContentException f) {
            throw new NoContentException(f.getLocalizedMessage());
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public Page<BuyModel> getAllBuyRecordsOfProduct(Long productId, HttpServletRequest req, Pageable pageable) {
        try {
            // checked the user is same user in this method
            var product = productService.getProduct(productId, req);
            return repo.findAllByProductId(product.getId(), pageable);
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public Page<BuyModel> getAllBuyRecordsOfUser(Long userId, HttpServletRequest req, Pageable pageable) {
        try {
            checkUserIsSameUserForRequest(null, userId, req, "fetch");
            return repo.findAllByProductCategoryUserId(userId, pageable);
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public BuyModel getBuy(Long buyId, HttpServletRequest req) {
        try {

            var foundBuyRecord = repo.findById(buyId);
            if (foundBuyRecord.isPresent()) {
                checkUserIsSameUserForRequest(foundBuyRecord.get().getProduct(), null, req, "fetch");
                return foundBuyRecord.get();
            }
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (NoContentException n) {
            throw new NoContentException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
        throw new NoContentException("Buy record do not exist.");
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public void deleteBuy(Long buyId, HttpServletRequest req) {
        try {
            var buy = repo.findById(buyId);
            if (buy.isPresent()) {
                checkUserIsSameUserForRequest(buy.get().getProduct(), null, req, "delete buy record of");
                repo.deleteById(buyId);
            } else {
                throw new NoContentException("Buy record does not exist");
            }
        } catch (NoContentException f) {
            throw new NoContentException(f.getLocalizedMessage());
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('OP_ACCESS_USER')")
    public Page<BuyModel> getBuysFromToDate(Long userId, LocalDateTime from, LocalDateTime to,
                                            HttpServletRequest req, Pageable pageable) {
        try {
            checkUserIsSameUserForRequest(null, userId, req, "fetch");
            return repo.findAllByProductCategoryUserIdAndCreatedAtAfterAndCreatedAtBefore(userId, from, to, pageable);
        } catch (ForbiddenException f) {
            throw new ForbiddenException(f.getLocalizedMessage());
        } catch (BadRequestException n) {
            throw new BadRequestException(n.getLocalizedMessage());
        } catch (NoContentException n) {
            throw new NoContentException(n.getLocalizedMessage());
        } catch (Exception e) {
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }


    private void checkUserIsSameUserForRequest(ProductModel product, Long userId, HttpServletRequest req,
                                               String operation) {
        var id = jwtUtils.getUserId(req.getHeader("refresh_token"));
        if (userId == null) {
            if (!product.getCategory().getUser().getId().equals(id))
                throw new ForbiddenException("You can't " + operation + " another user's products");

        } else if (!userId.equals(id))
            throw new ForbiddenException("You can't " + operation + " another user's products");

    }

    private void addProductCount(BuyModel buy, HttpServletRequest req) {
        var preProduct = productService.getProduct(buy.getProduct().getId(), req);
        var product = new ProductModel();
        checkUserIsSameUserForRequest(preProduct, null, req, "save buy record of");
        product.setTotalCount(preProduct.getTotalCount().add(buy.getCount()));
        var productId = preProduct.getId();
        productService.updateProduct(product, preProduct, productId, req);
    }

}