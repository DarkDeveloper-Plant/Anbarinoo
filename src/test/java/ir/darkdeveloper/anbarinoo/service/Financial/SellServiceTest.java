package ir.darkdeveloper.anbarinoo.service.Financial;

import ir.darkdeveloper.anbarinoo.TestUtils;
import ir.darkdeveloper.anbarinoo.exception.BadRequestException;
import ir.darkdeveloper.anbarinoo.exception.NoContentException;
import ir.darkdeveloper.anbarinoo.model.CategoryModel;
import ir.darkdeveloper.anbarinoo.model.SellModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.service.CategoryService;
import ir.darkdeveloper.anbarinoo.service.ProductService;
import ir.darkdeveloper.anbarinoo.service.UserService;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext
public record SellServiceTest(UserService userService,
                              JwtUtils jwtUtils,
                              ProductService productService,
                              SellService sellService,
                              CategoryService categoryService,
                              TestUtils testUtils) {

    private static HttpServletRequest request;
    private static Long userId;
    private static Long sellId;
    private static Long catId;
    private static Long productId;
    private static Pageable pageable;

    @Autowired
    public SellServiceTest {
    }


    @BeforeAll
    static void setUp() {
        pageable = PageRequest.of(0, 8);
    }


    @Test
    @Order(1)
    @WithMockUser(username = "anonymousUser")
    void saveUser() {
        var response = mock(HttpServletResponse.class);
        var user = UserModel.builder()
                .email("email@mail.com")
                .address("address")
                .description("desc")
                .userName("user n")
                .enabled(true)
                .password("pass12B~")
                .passwordRepeat("pass12B~")
                .build();
        userService.signUpUser(Optional.of(user), response);
        userId = user.getId();
        request = testUtils.setUpHeaderAndGetReq(user.getEmail(), userId);
    }

    @Test
    @Order(2)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void saveCategory() {
        var electronics = new CategoryModel("Electronics");
        categoryService.saveCategory(electronics, request);
        catId = electronics.getId();
    }

    @Test
    @Order(3)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void saveProduct() {
        var product = ProductModel.builder()
                .name("name")
                .description("description")
                .totalCount(BigDecimal.valueOf(50))
                .price(BigDecimal.valueOf(56))
                .category(new CategoryModel(catId))
                .tax(9)
                .build();
        productService.saveProduct(Optional.of(product), request);
        productId = product.getId();
    }

    @Test
    @Order(4)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void saveSell() {
        var sellRecord = SellModel.builder()
                .product(new ProductModel(productId))
                .price(BigDecimal.valueOf(50))
                .count(BigDecimal.valueOf(20))
                .tax(9)
                .build();
        sellService.saveSell(Optional.of(sellRecord), request);
        sellId = sellRecord.getId();
        var fetchedSell = sellService.getSell(sellId, request);
        assertThat(fetchedSell.getProduct()).isNotNull();
        assertThat(fetchedSell.getProduct().getCategory().getUser().getId()).isEqualTo(userId);
    }

    @Test
    @Order(5)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void updateSellWithNullUpdatableValues() {
        var sellRecord = SellModel.builder()
                .product(new ProductModel(productId))
                .price(null)
                .count(null)
                .build();
        assertThrows(BadRequestException.class, () -> {
            sellService.updateSell(Optional.of(sellRecord), sellId, request);
            var fetchedSell = sellService.getSell(sellId, request);
            assertThat(fetchedSell.getCount()).isEqualTo(BigDecimal.valueOf(200000, 4));
            assertThat(fetchedSell.getPrice()).isEqualTo(BigDecimal.valueOf(500000, 4));
        });
    }

    @Test
    @Order(6)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void updateSell() {
        var sellRecord = SellModel.builder()
                .product(new ProductModel(productId))
                .price(BigDecimal.valueOf(60.505))
                .count(BigDecimal.valueOf(26.502))
                .build();
        sellService.updateSell(Optional.of(sellRecord), sellId, request);
        var fetchedSell = sellService.getSell(sellId, request);
        assertThat(fetchedSell.getCount()).isEqualTo(BigDecimal.valueOf(265020, 4));
        assertThat(fetchedSell.getPrice()).isEqualTo(BigDecimal.valueOf(605050, 4));
    }

    @Test
    @Order(7)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void getAllSellRecordsOfProduct() {
        var fetchedRecords = sellService.getAllSellRecordsOfProduct(productId, request, pageable);
        assertThat(fetchedRecords.getContent().get(0).getId()).isEqualTo(sellId);
        assertThat(fetchedRecords.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(605050, 4));
    }

    @Test
    @Order(8)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void getAllSellRecordsOfUser() {
        var fetchedRecords = sellService.getAllSellRecordsOfUser(userId, request, pageable);
        assertThat(fetchedRecords.getContent().get(0).getId()).isEqualTo(sellId);
        assertThat(fetchedRecords.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(605050, 4));
    }

    @Test
    @Order(9)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void deleteSell() {
        sellService.deleteSell(sellId, request);
        assertThrows(NoContentException.class, () -> sellService.getSell(sellId, request));
        var product = productService.getProduct(productId, request);
        assertThat(product.getId()).isEqualTo(productId);
    }


}