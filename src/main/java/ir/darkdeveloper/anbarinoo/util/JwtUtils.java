package ir.darkdeveloper.anbarinoo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

@Service
@Getter
@Setter
public class JwtUtils {

    // defined in application.properties or application.yml
    @Value("${jwt.secretKey}")
    private String secret;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final PasswordEncoder encoder;
    private Long refreshExpire;
    private Long accessExpire;

    @Autowired
    public JwtUtils(PasswordEncoder encoder) {
        this.encoder = encoder;
        refreshExpire = (long) (60 * 60 * 24 * 7 * 3 * 1000);
        accessExpire = (long) (60 * 1000);
    }

    @PostConstruct
    public void initSecret() {
        // encodes the jwt secret
        // note that previous token after restarting application won't work
        secret = encoder.encode(secret);
    }

    // generates a unique jwt token
    public String generateRefreshToken(String username, Long userId) {
        // expires in 21 days
        var date = new Date(System.currentTimeMillis() + refreshExpire);
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, secret)
                .setIssuedAt(new Date())
                .setSubject(username)
                .claim("user_id", userId).setExpiration(date).compact();
    }

    // Generates access token
    public String generateAccessToken(String username) {
        var date = new Date(System.currentTimeMillis() + accessExpire);
        return Jwts.builder().signWith(SignatureAlgorithm.HS256, secret).setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(date).compact();
    }

    public String getUsername(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
    }

    public Long getUserId(String refreshToken) {
        return ((Integer) getAllClaimsFromToken(refreshToken).get("user_id")).longValue();
    }

    public Claims getAllClaimsFromToken(String token) throws JwtException {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    public Boolean isTokenExpired(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return false;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return true;
    }

    public LocalDateTime getExpirationDate(String token) throws JwtException {
        return getClaimFromToken(token, Claims::getExpiration)
                .toInstant().atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    // for testing purposes
    public void changeExpirationDate(String token, long till) {
        Jwts.claims(getAllClaimsFromToken(token)).setExpiration(new Date(till));
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        var claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
}
