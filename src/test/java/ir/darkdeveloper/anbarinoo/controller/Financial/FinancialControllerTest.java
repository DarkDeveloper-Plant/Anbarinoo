package ir.darkdeveloper.anbarinoo.controller.Financial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.darkdeveloper.anbarinoo.model.CategoryModel;
import ir.darkdeveloper.anbarinoo.model.Financial.BuyModel;
import ir.darkdeveloper.anbarinoo.model.Financial.SellModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.service.CategoryService;
import ir.darkdeveloper.anbarinoo.service.Financial.BuyService;
import ir.darkdeveloper.anbarinoo.service.Financial.DebtOrDemandService;
import ir.darkdeveloper.anbarinoo.service.Financial.SellService;
import ir.darkdeveloper.anbarinoo.service.ProductService;
import ir.darkdeveloper.anbarinoo.service.UserService;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import ir.darkdeveloper.anbarinoo.util.UserUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public record FinancialControllerTest(UserService userService,
                                      ProductService productService,
                                      JwtUtils jwtUtils,
                                      WebApplicationContext webApplicationContext,
                                      CategoryService categoryService,
                                      BuyService buyService,
                                      SellService sellService,
                                      DebtOrDemandService dodService) {

    private static int saveBuyCounter = 0;
    private static Long userId;
    private static String refresh;
    private static String access;
    private static Long productId;
    private static Long buyId;
    private static Long catId;
    private static HttpServletRequest request;
    private static MockMvc mockMvc;
    private static LocalDateTime fromDate = null;
    private static LocalDateTime toDate = null;

    @Autowired
    public FinancialControllerTest {
    }

    @BeforeAll
    static void setUp() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        request = mock(HttpServletRequest.class);
    }

    @BeforeEach
    void setUp2() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @WithMockUser(username = "anonymousUser")
    void saveUser() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        var user = new UserModel();
        user.setEmail("email@mail.com");
        user.setAddress("address");
        user.setDescription("desc");
        user.setUserName("user n");
        user.setPassword("pass1");
        user.setPasswordRepeat("pass1");
        user.setEnabled(true);
        userService.signUpUser(user, response);
        userId = user.getId();
        request = setUpHeader(user.getEmail(), userId);
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
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void saveProduct() {
        var product = new ProductModel();
        product.setName("name");
        product.setDescription("description");
        product.setTotalCount(BigDecimal.valueOf(50));
        product.setPrice(BigDecimal.valueOf(500));
        product.setCategory(new CategoryModel(catId));
        productService.saveProduct(product, request);
        productId = product.getId();
    }

    @RepeatedTest(5)
    @Order(4)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void saveBuy() throws InterruptedException {
        var buy = new BuyModel();
        buy.setProduct(new ProductModel(productId));
        buy.setPrice(BigDecimal.valueOf(5000));
        buy.setCount(BigDecimal.valueOf(8));
        buyService.saveBuy(buy, request);
        assertThat(buy.getId()).isNotNull();
        if (fromDate == null) {
            fromDate = LocalDateTime.now();
            Thread.sleep(3000);
        }
        Thread.sleep(1000);
    }


    @RepeatedTest(5)
    @Order(5)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void saveSell() throws InterruptedException {
        var sell = new SellModel();
        sell.setProduct(new ProductModel(productId));
        sell.setPrice(BigDecimal.valueOf(6000));
        sell.setCount(BigDecimal.valueOf(4));
        sellService.saveSell(sell, request);
        assertThat(sell.getId()).isNotNull();
        if (fromDate == null)
            fromDate = LocalDateTime.now();

        Thread.sleep(1000);
    }

    @Test
    @Order(6)
    @WithMockUser(authorities = "OP_ACCESS_USER")
    void getProductsAfterSells() {
        var product = productService.getProduct(productId, request);
        assertThat(product.getTotalCount()).isEqualTo(BigDecimal.valueOf(7000, 2));
    }


    @Test
    void getCosts() {
    }

    @Test
    void getIncomes() {
    }


    private String mapToJson(Object obj) throws JsonProcessingException {
        return new ObjectMapper().findAndRegisterModules().writeValueAsString(obj);
    }

    //should return the object; data is being removed
    private HttpServletRequest setUpHeader(String email, Long userId) {

        Map<String, String> headers = new HashMap<>();
        headers.put(null, "HTTP/1.1 200 OK");
        headers.put("Content-Type", "text/html");

        refresh = jwtUtils.generateRefreshToken(email, userId);
        access = jwtUtils.generateAccessToken(email);
        var refreshDate = UserUtils.TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(refresh));
        var accessDate = UserUtils.TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(access));
        headers.put("refresh_token", refresh);
        headers.put("access_token", access);
        headers.put("refresh_expiration", refreshDate);
        headers.put("access_expiration", accessDate);


        HttpServletRequest request = mock(HttpServletRequest.class);
        for (String key : headers.keySet())
            when(request.getHeader(key)).thenReturn(headers.get(key));

        return request;
    }
}