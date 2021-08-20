package ir.darkdeveloper.anbarinoo.service.Financial;

import ir.darkdeveloper.anbarinoo.exception.NoContentException;
import ir.darkdeveloper.anbarinoo.model.Financial.SellsModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import ir.darkdeveloper.anbarinoo.service.ProductService;
import ir.darkdeveloper.anbarinoo.service.UserService;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import ir.darkdeveloper.anbarinoo.util.UserUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public record SellsServiceTest(UserService userService,
                               JwtUtils jwtUtils,
                               ProductService productService,
                               SellsService sellsService) {

    private static HttpServletRequest request;
    private static Long userId;
    private static Long sellId;
    private static Long productId;
    private static Pageable pageable;

    @Autowired
    public SellsServiceTest {
    }


    @BeforeAll
    static void setUp() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        request = mock(HttpServletRequest.class);
        pageable = PageRequest.of(0, 8);
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
    void saveProduct() {
        var product = new ProductModel();
        product.setName("name");
        product.setDescription("description");
        product.setTotalCount(50);
        product.setPrice(BigDecimal.valueOf(56));
        productService.saveProduct(product, request);
        productId = product.getId();
    }


    @Test
    @Order(3)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void saveSell() {
        var sellRecord = new SellsModel();
        var product = new ProductModel();
        product.setId(productId);
        sellRecord.setCount(BigDecimal.valueOf(20));
        sellRecord.setPrice(BigDecimal.valueOf(50));
        sellRecord.setProduct(product);
        sellsService.saveSell(sellRecord, request);
        sellId = sellRecord.getId();
        var fetchedSell = sellsService.getSell(sellId, request);
        assertThat(fetchedSell.getProduct()).isNotNull();
        assertThat(fetchedSell.getProduct().getUser().getId()).isEqualTo(userId);
    }

    @Test
    @Order(4)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void updateSellWithNullUpdatableValues() {
        var sellRecord = new SellsModel();
        var product = new ProductModel();
        product.setId(productId);
        sellRecord.setCount(null);
        sellRecord.setPrice(null);
        sellRecord.setProduct(product);
        sellsService.updateSell(sellRecord, sellId, request);
        var fetchedSell = sellsService.getSell(sellId, request);
        assertThat(fetchedSell.getCount()).isEqualTo(BigDecimal.valueOf(200000, 4));
        assertThat(fetchedSell.getPrice()).isEqualTo(BigDecimal.valueOf(500000, 4));
    }

    @Test
    @Order(5)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void updateSell() {
        var sellRecord = new SellsModel();
        var product = new ProductModel();
        product.setId(productId);
        sellRecord.setCount(BigDecimal.valueOf(26.502));
        sellRecord.setPrice(BigDecimal.valueOf(60.505));
        sellRecord.setProduct(product);
        sellsService.updateSell(sellRecord, sellId, request);
        var fetchedSell = sellsService.getSell(sellId, request);
        assertThat(fetchedSell.getCount()).isEqualTo(BigDecimal.valueOf(265020, 4));
        assertThat(fetchedSell.getPrice()).isEqualTo(BigDecimal.valueOf(605050, 4));
    }

    @Test
    @Order(6)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void getAllSellRecordsOfProduct() {
        var fetchedRecords = sellsService.getAllSellRecordsOfProduct(productId, request, pageable);
        assertThat(fetchedRecords.getContent().get(0).getId()).isEqualTo(sellId);
        assertThat(fetchedRecords.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(605050, 4));
    }

    @Test
    @Order(7)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void getAllSellRecordsOfUser() {
        var fetchedRecords = sellsService.getAllSellRecordsOfUser(userId, request, pageable);
        assertThat(fetchedRecords.getContent().get(0).getId()).isEqualTo(sellId);
        assertThat(fetchedRecords.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(605050, 4));
    }

    @Test
    @Order(8)
    @WithMockUser(authorities = {"OP_ACCESS_USER"})
    void deleteSell() {
        sellsService.deleteSell(sellId, request);
        assertThrows(NoContentException.class, () -> sellsService.getSell(sellId, request));
        var product = productService.getProduct(productId, request);
        assertThat(product.getId()).isEqualTo(productId);
    }


    //should return the object; data is being removed
    private HttpServletRequest setUpHeader(String email, Long userId) {

        Map<String, String> headers = new HashMap<>();
        headers.put(null, "HTTP/1.1 200 OK");
        headers.put("Content-Type", "text/html");

        String refreshToken = jwtUtils.generateRefreshToken(email, userId);
        String accessToken = jwtUtils.generateAccessToken(email);
        var refreshDate = UserUtils.TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(refreshToken));
        var accessDate = UserUtils.TOKEN_EXPIRATION_FORMAT.format(jwtUtils.getExpirationDate(accessToken));
        headers.put("refresh_token", refreshToken);
        headers.put("access_token", accessToken);
        headers.put("refresh_expiration", refreshDate);
        headers.put("access_expiration", accessDate);


        HttpServletRequest request = mock(HttpServletRequest.class);
        for (String key : headers.keySet())
            when(request.getHeader(key)).thenReturn(headers.get(key));

        return request;
    }


}