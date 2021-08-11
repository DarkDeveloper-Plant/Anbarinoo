package ir.darkdeveloper.anbarinoo.service;

import ir.darkdeveloper.anbarinoo.exception.ForbiddenException;
import ir.darkdeveloper.anbarinoo.exception.InternalServerException;
import ir.darkdeveloper.anbarinoo.util.JwtUtils;
import ir.darkdeveloper.anbarinoo.util.UserUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ir.darkdeveloper.anbarinoo.model.CategoryModel;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.model.UserModel;
import org.springframework.security.test.context.support.WithMockUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public record ProductServiceTest(ProductService productService,
                                 JwtUtils jwtUtils,
                                 UserService userService,
                                 CategoryService categoryService) {


    private static HttpServletRequest request;
    private static CategoryModel cat1;
    private static CategoryModel electronics;
    private static Long userId;
    private static Long userId2;
    private static Long productId;

    @Autowired
    public ProductServiceTest {
    }

    @BeforeAll
    static void setUp() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        request = mock(HttpServletRequest.class);
        System.out.println("ProductServiceTest.setUp");
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
        var user2 = new UserModel();
        user2.setEmail("email2@mail.com");
        user2.setPassword("pass1");
        user2.setPasswordRepeat("pass1");
        userService.signUpUser(user2, response);
        userId2 = user2.getId();
        System.out.println("ProductServiceTest.saveUser");
    }

    @Test
    @Order(2)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void saveCategory() {
        System.out.println("ProductServiceTest.saveCategory");
        cat1 = new CategoryModel("Other");
        categoryService.saveCategory(cat1, request);
        electronics = new CategoryModel("Electronics");
        CategoryModel mobilePhones = new CategoryModel("Mobile phones", electronics);
        CategoryModel washingMachines = new CategoryModel("Washing machines", electronics);
        electronics.addChild(mobilePhones);
        electronics.addChild(washingMachines);
        CategoryModel iPhone = new CategoryModel("iPhone", mobilePhones);
        CategoryModel samsung = new CategoryModel("Samsung", mobilePhones);
        mobilePhones.addChild(iPhone);
        mobilePhones.addChild(samsung);
        CategoryModel galaxy = new CategoryModel("Galaxy", samsung);
        samsung.addChild(galaxy);

        categoryService.saveCategory(electronics, request);
    }

    @Test
    @Order(3)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void saveProduct() {
        System.out.println("ProductServiceTest.saveProduct");
        var product = new ProductModel();
        product.setName("name");
        product.setDescription("description");
        product.setBoughtCount(25);
        product.setBuyPrice(156d);
        product.setSoldCount(13);
        product.setTotalCount(50);
        MockMultipartFile file3 = new MockMultipartFile("file", "hello.jpg", MediaType.IMAGE_JPEG_VALUE,
                "Hello, World!".getBytes());
        MockMultipartFile file4 = new MockMultipartFile("file", "hello.jpg", MediaType.IMAGE_JPEG_VALUE,
                "Hello, World!".getBytes());
        product.setFiles(Arrays.asList(file3, file4));
        product.setCategory(cat1);
        productService.saveProduct(product, request);
        productId = product.getId();
    }

    @Test
    @Order(4)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void getProduct() {
        System.out.println("ProductServiceTest.getProduct");
        var fetchedProduct = productService.getProduct(productId, request);
        assertThat(fetchedProduct.getUser().getId()).isEqualTo(userId);
    }

    @Test
    @Order(5)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void updateProduct() {
        var product = new ProductModel();
        product.setName("updatedName");
        product.setDescription("updatedDescription");
        product.setBoughtCount(15);
        product.setBuyPrice(25d);
        product.setSoldCount(6);
        product.setTotalCount(10);
        product.setCategory(electronics);

        productService.updateProduct(product, productId, request);
        var fetchedProduct = productService.getProduct(productId, request);
        assertThat(fetchedProduct.getCategory().getName())
                .isEqualTo(product.getCategory().getName());

    }

    @Test
    @Order(6)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void updateProductImages() {
        var product = new ProductModel();

        MockMultipartFile file3 = new MockMultipartFile("file", "helladsfo.jpg", MediaType.IMAGE_JPEG_VALUE,
                "Hello, World!".getBytes());
        MockMultipartFile file4 = new MockMultipartFile("file", "heladsflo.jpg", MediaType.IMAGE_JPEG_VALUE,
                "Hello, World!".getBytes());
        MockMultipartFile file5 = new MockMultipartFile("file", "heladsflo.jpg", MediaType.IMAGE_JPEG_VALUE,
                "Hello, World!".getBytes());
//        MockMultipartFile file6 = new MockMultipartFile("file", "heladsflo.jpg", MediaType.IMAGE_JPEG_VALUE,
//                "Hello, World!".getBytes());
        product.setFiles(Arrays.asList(file3, file4, file5/*, file6*/));

        productService.updateProductImages(product, productId, request);

        var fetchedProduct = productService.getProduct(productId, request);
        assertThat(fetchedProduct.getImages().size()).isNotEqualTo(0);
        for (var image : fetchedProduct.getImages())
            assertThat(image).isNotEqualTo("noImage.png");
    }

    @Test
    @Order(7)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    @Disabled
    void deleteAllUpdateProductImages() {
        var product = new ProductModel();
        var fetchedProduct = productService.getProduct(productId, request);
        product.setImages(fetchedProduct.getImages());

        productService.updateDeleteProductImages(product, productId, request);

        var fetchedProduct2 = productService.getProduct(productId, request);
        assertThat(fetchedProduct2.getImages().size()).isNotEqualTo(0);
        for (var image : fetchedProduct2.getImages())
            assertThat(image).isEqualTo("noImage.png");
    }

    @Test
    @Order(8)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void delete3UpdateProductImages() {
        var product = new ProductModel();
        var fetchedProduct = productService.getProduct(productId, request);
        var fileNames = fetchedProduct.getImages();
        fileNames.remove(0);
        fileNames.remove(1);
        product.setImages(fileNames);

        productService.updateDeleteProductImages(product, productId, request);

        var fetchedProduct2 = productService.getProduct(productId, request);
        assertThat(fetchedProduct2.getImages().size()).isEqualTo(2);
        for (var image : fetchedProduct2.getImages())
            assertThat(image).isNotEqualTo("noImage.png");
    }


    @Test
    @Order(9)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void findByNameContains() {
        System.out.println("ProductServiceTest. findByNameContains");
        var pageable = PageRequest.of(0, 8);
        var product = new ProductModel();
        product.setName("updatedName");
        product.setBoughtCount(15);
        var foundProducts = productService.findByNameContains(product.getName().substring(0, 2), pageable, request);
        foundProducts.getContent().forEach(p -> {
            assertThat(p.getName()).isEqualTo(product.getName());
            assertThat(p.getBoughtCount()).isEqualTo(product.getBoughtCount());
        });
    }

    @Test
    @Order(10)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void getOneUserProducts() {
        var pageable = PageRequest.of(0, 8);
        var product = new ProductModel();
        product.setName("updatedName");
        product.setBoughtCount(15);
        assertThrows(ForbiddenException.class, () -> {
            var products = productService.getOneUserProducts(userId2, pageable, request);
            products.getContent().forEach(p -> {
                assertThat(p.getName()).isEqualTo(product.getName());
                assertThat(p.getBoughtCount()).isEqualTo(product.getBoughtCount());
            });
        });
    }

    @Test
    @Order(11)
    @WithMockUser(username = "email@mail.com", authorities = {"OP_ACCESS_USER"})
    void deleteProduct() {
        productService.deleteProduct(productId, request);
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
